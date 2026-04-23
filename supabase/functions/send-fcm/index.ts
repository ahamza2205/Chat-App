import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "npm:@supabase/supabase-js@2"
import { encode as base64url } from "https://deno.land/std@0.168.0/encoding/base64url.ts"

async function getAccessToken(sa: any): Promise<string> {
  const now = Math.floor(Date.now() / 1000)
  const header = { alg: "RS256", typ: "JWT" }
  const payload = {
    iss: sa.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  }

  const enc = new TextEncoder()
  const headerB64 = base64url(enc.encode(JSON.stringify(header)))
  const payloadB64 = base64url(enc.encode(JSON.stringify(payload)))
  const unsigned = `${headerB64}.${payloadB64}`

  const pemBody = sa.private_key
    .replace("-----BEGIN PRIVATE KEY-----", "")
    .replace("-----END PRIVATE KEY-----", "")
    .replace(/\n/g, "")
  const binaryKey = Uint8Array.from(atob(pemBody), (c: string) => c.charCodeAt(0))

  const cryptoKey = await crypto.subtle.importKey(
    "pkcs8", binaryKey,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false, ["sign"],
  )

  const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", cryptoKey, enc.encode(unsigned))
  const jwt = `${unsigned}.${base64url(new Uint8Array(signature))}`

  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=${jwt}`,
  })
  const data = await res.json()
  if (!res.ok) throw new Error(`Token exchange failed: ${JSON.stringify(data)}`)
  return data.access_token
}

async function sendFcm(
  projectId: string, accessToken: string,
  token: string, dataPayload: Record<string, string>,
): Promise<boolean> {
  const res = await fetch(
    `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        message: { token, data: dataPayload, android: { priority: "high" } },
      }),
    },
  )
  if (!res.ok) {
    const err = await res.json()
    console.error(`FCM failed for ${token.slice(0, 10)}:`, err)
  }
  return res.ok
}

// Cache the access token across requests within the same worker
let cachedToken: string | null = null
let tokenExpiry = 0

serve(async (req: Request) => {
  try {
    const body = await req.json()
    const record = body?.record
    if (!record) return new Response("No record", { status: 400 })

    const senderId: string = record.sender_id ?? ""
    const senderName: string = record.sender_name ?? "Someone"
    const text: string = (record.text ?? "").trim()

    // Detect media — attachments is a JSON string like '[{"id":"...","remoteUrl":"...","mimeType":"audio/mp4"}]'
    let hasMedia = false
    let hasAudio = false
    try {
      const raw = record.attachments
      const att = typeof raw === "string" ? JSON.parse(raw) : raw
      if (Array.isArray(att) && att.length > 0) {
        hasMedia = true
        hasAudio = att.some((a: any) => typeof a.mimeType === "string" && a.mimeType.startsWith("audio/"))
      }
    } catch { /* ignore parse errors */ }

    // Build preview: text > audio label > photo label > fallback
    let preview: string
    if (text.length > 0) {
      preview = text.slice(0, 200)
    } else if (hasAudio) {
      preview = "Voice note"
    } else if (hasMedia) {
      preview = "Photo"
    } else {
      preview = "New message"
    }

    console.log(`Processing: sender=${senderName}, text=${!!text}, media=${hasMedia}, preview=${preview.slice(0, 30)}`)

    // Load service account (parse once, handle double-encoding)
    const saRaw = Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON")
    if (!saRaw) throw new Error("Missing FIREBASE_SERVICE_ACCOUNT_JSON")
    let sa: any = JSON.parse(saRaw)
    if (typeof sa === "string") sa = JSON.parse(sa)

    // Reuse access token if still valid
    const now = Math.floor(Date.now() / 1000)
    if (!cachedToken || now >= tokenExpiry) {
      cachedToken = await getAccessToken(sa)
      tokenExpiry = now + 3500 // refresh 100s before expiry
    }

    // Fetch recipient tokens (everyone except sender)
    const supabase = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "",
    )
    const { data: rows, error } = await supabase
      .from("user_tokens")
      .select("fcm_token")
      .neq("user_id", senderId)

    if (error) { console.error("DB error:", error); return new Response("DB error", { status: 500 }) }
    if (!rows?.length) return new Response("No recipients", { status: 200 })

    // Send data-only push to each recipient
    const dataPayload: Record<string, string> = {
      sender_name: senderName,
      text: preview,
      is_media: hasMedia ? "true" : "false",
    }

    let ok = 0
    for (const row of rows) {
      if (await sendFcm(sa.project_id, cachedToken!, row.fcm_token, dataPayload)) ok++
    }

    console.log(`FCM: ${ok}/${rows.length} delivered`)
    return new Response(JSON.stringify({ ok, total: rows.length }), {
      headers: { "Content-Type": "application/json" },
    })
  } catch (err: any) {
    console.error("send-fcm error:", err.message)
    return new Response(err.message, { status: 500 })
  }
})
