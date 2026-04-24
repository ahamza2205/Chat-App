# Real-Time Chat App

A production-minded real-time chat application built with **Clean Architecture + MVI**, featuring reliable offline-first message delivery, media support, background processing, push notifications, and a scalable architecture designed for future growth.

This project was built as a take-home technical assessment with strong focus on:

* Architecture quality
* Reliability
* Maintainability
* Real-world messaging behavior
* User experience polish

---

## Features

### Core Messaging

* Real-time shared global chat room
* Send and receive text messages
* Offline-first local persistence using Room
* Reliable message delivery with retry support
* Message status handling:
  * Sending
  * Sent
  * Failed

🎥 Demo Video: [Watch Core Messaging Demo](https://drive.google.com/file/d/1qsd2g1C7pIwAkGm7oM_XwpLUQ9NEA6Oh/view?usp=sharing)

---

### Media Messages

* Send up to 10 images at a time
* Media preview before sending
* Background media upload using WorkManager
* Retry failed uploads safely
* Ongoing upload notifications

🎥 Demo Video: [Watch Media Messages Demo](https://drive.google.com/file/d/1u2dKZIG3DEaEl18UXpJuRm5Q7p2i2KLf/view?usp=sharing)

---

### Voice Notes

* Record voice notes directly from chat
* Audio preview before sending
* Background upload using the existing sending pipeline
* Audio message rendering inside chat

🎥 Demo Video: [Watch Voice Notes Demo](https://drive.google.com/file/d/1DAbw1q4RJWZbvlC6DQ1yMYzmpAXZsV4i/view?usp=sharing)

---

### Reply to Message

* WhatsApp-style swipe-to-reply
* Reply preview above input field
* Reply block rendered inside message bubble
* Reply snapshot preserved for consistency

🎥 Demo Video: [Watch Reply Feature Demo](https://drive.google.com/file/d/1Ur2QiqNrmSeXrAkMhPrPuqkZJAJdaCP4/view?usp=sharing)

---

### Message Deletion

#### Delete for Me

* Removes the message locally for the current user only

#### Delete for Everyone

* Soft delete for all users
* Replaced with:
  `This message was deleted`

🎥 Demo Video: [Watch Delete Feature Demo](https://drive.google.com/file/d/15wvQJmnw3GCfOqARKZPt8oqoewzT4py1/view?usp=sharing)

---

### User Identity

* Username + profile image
* Unique persisted device identity
* Installation-based UUID stored in DataStore
* No login/auth required

🎥 Demo Video: [Watch Profile Flow Demo](https://drive.google.com/file/d/1d1LpvgjXHY0K3U2bEG1VJUhYhixx7VP0/view?usp=sharing)

---

### Notifications

#### Background Work Notifications

* Sending message notifications
* Uploading media notifications
* Retry / Cancel actions

#### Push Notifications (FCM)

* New incoming message notifications
* Works in background and closed app
* Powered by Firebase Cloud Messaging + Supabase Edge Function

🎥 Demo Video: [Watch Notifications Demo](https://drive.google.com/file/d/1d61zGQJ2Clq264-PIWADmWja_Vz0sdsz/view?usp=sharing)

---

## Architecture

### Clean Architecture

The project follows strict Clean Architecture separation:

#### Presentation

* Jetpack Compose UI
* ViewModels
* MVI State + Intents + Effects

#### Domain

* Models
* Repository contracts
* Use cases

#### Data

* Room
* Supabase
* WorkManager
* Firebase
* Repository implementations

This keeps the app scalable, testable, and maintainable.

---

### Why Room is the Single Source of Truth

Even when using Supabase Realtime, UI never renders directly from network callbacks.

Instead:

```
Supabase → Room → Flow → ViewModel → UI
```

This ensures:

* Offline-first behavior
* Reliable retries
* Stable UI state
* Easier testing
* Deduplication safety

---

### Why WorkManager is the Executor

All sending operations (text, images, audio) go through WorkManager.

Why:

* Survives process death
* Supports retries with exponential backoff
* Foreground notifications
* Reliable background execution

**WorkManager executes. Room stores state. UI renders state.**

---

## Tech Stack

* Kotlin
* Jetpack Compose
* Material 3
* Hilt
* Room
* DataStore
* Coroutines + Flow
* WorkManager
* Supabase (Realtime, Postgrest, Storage)
* Firebase Cloud Messaging
* Media3 / MediaPlayer (for audio)
* JUnit + MockK
* Compose UI Testing

---

## Permissions Strategy

The app follows secure scoped-access best practices.

### Storage

No broad storage permissions used.

**Not used:**
* `READ_EXTERNAL_STORAGE`
* `WRITE_EXTERNAL_STORAGE`
* `MANAGE_EXTERNAL_STORAGE`

**Uses instead:**
* Modern media picker (`PickVisualMedia`)
* Scoped storage
* App-private copied media for reliable retries

### Microphone

Only minimal microphone permission for voice note recording.

### Notifications

`POST_NOTIFICATIONS` handled at runtime for Android 13+.

---

## Important Design Decisions

### Generic Attachment Model

The attachment model supports generic media via MIME type.

Examples:
* `image/jpeg`
* `audio/mpeg`
* `video/mp4` (future-ready)

For this implementation, **images + audio were prioritized**. Video support was intentionally not implemented to keep scope focused and quality high.

This keeps the architecture extensible without unnecessary complexity.

---

### Device Identity Instead of Authentication

Users are uniquely identified using a persisted installation UUID.

Why:
* Required by task specification
* Avoids unnecessary auth complexity
* Keeps onboarding instant

Identity fields on every message:
* `senderId` → persisted device UUID
* `senderName` → user profile name
* `senderAvatarUrl` → user profile image

---

## Testing

### Unit Tests

Coverage includes:

* `SendMessageWorker` — retry logic, failure handling, edge cases
* `ChatRepositoryImpl` — insert, retry, cancel flows
* `ChatViewModel` — intent handling, state updates
* `MessageMapper` — entity ↔ domain mapping

**14 unit tests passing.**

---

### UI / Instrumentation Tests

Coverage includes:

* Chat screen rendering
* Sending messages
* Reply preview
* Delete actions
* Deleted placeholder rendering

**6 stable Compose UI tests.**

---

## Setup

### Requirements

* Android Studio Hedgehog+
* JDK 17
* Firebase project
* Supabase project

---

### Firebase

Add your `google-services.json` inside:

```
app/
```

---

### Supabase

Configure the following in your `local.properties` or `BuildConfig`:

* Supabase project URL
* Supabase anon key
* Storage bucket (`attachments`)
* Realtime table (`messages`)
* Edge Function for FCM push dispatch

---

## Known Limitations

* Single global chat room only
* No private conversations
* No authentication system
* No video attachments yet
* Voice note playback kept intentionally simple
* No advanced waveform/seekbar system

These were intentional scope decisions.

---

## Future Improvements

* Multiple chat rooms
* Private conversations
* Video attachments
* Message reactions
* Read receipts
* Typing indicators
* Better voice-note playback UI
* Advanced moderation/admin tools

---

## Final Notes

This project intentionally prioritizes:

* **Reliability** over shortcuts
* **Architecture** over hacks
* **Maintainability** over over-engineering

The goal was not just to make features work, but to make them **production-minded and scalable**.
