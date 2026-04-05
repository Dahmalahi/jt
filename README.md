# temporal
<img width="96" height="96" alt="Temporal" src="https://github.com/user-attachments/assets/b92a95e1-adb3-469f-8dbe-701a97313e9d" />
> a micro-blogging J2ME client — CLDC 1.1 / MIDP 2.0

**version 1.3 mod** · by **aditya** and DASH ANIMATION V2 [Dash Animation V2 on YouTube](https://www.youtube.com/@dash______animationv2)

---

## what is this?

temporal is a lightweight social micro-blogging app built for old-school Java ME phones. it runs entirely on CLDC 1.1 and MIDP 2.0 — no modern smartphone required. think early 2000s Nokia vibes, but posting to a real REST API.

backend:`temporal.dedomil.workers.dev`

---

## source files

| file | description |
|---|---|
| `Temporal.java` | main MIDlet, navigation controller |
| `LoginScreen.java` | login + register form (toggles between modes) |
| `HomeScreen.java` | global timeline list screen |
| `PostScreen.java` | full post view |
| `CreatePostScreen.java` | compose new post (TextBox) |
| `ProfileScreen.java` | your own posts *(new in v1.1)* |
| `AboutScreen.java` | credits and links *(new in v1.1)* |
| `Service.java` | HTTP layer (GET / POST / DELETE) |
| `RMSStore.java` | persistent storage via MIDP RMS |
| `JSON.java` | minimal JSON parser (no reflection, no libraries) |

---

## what's new in v1.3

### bug fixes

**HomeScreen was showing your own posts instead of the global timeline.**
the original code sent requests to `/u/{username}` by default, meaning you only ever saw your own posts after logging in. this has been moved to `ProfileScreen`. `HomeScreen` now correctly hits `/posts?page=N` for the public timeline.

**clicking a post before it loaded caused a NullPointerException.**
the select handler now checks `isLoading` and guards against a null `posts` vector before accessing elements.

**post ID was wrong in PostScreen.**
`showPost()` was being called with `idx + ""` (the list index), not `post[0]` (the actual server-side post ID). deleting a post or any future ID-dependent feature would have targeted the wrong post. now uses `post[0]`.

**JSON parser crashed on posts containing `{` or `}` in the content.**
the original `parseArray` found the next `}` with `indexOf`, not caring about nesting depth. any post body with a brace (like `{"formatted": "like this"}`) would silently break the parser. v1.1 tracks brace depth and respects string boundaries.

**error messages were hidden.**
`HomeScreen.onError` always showed `"error: api error"` regardless of what the server said. the actual error message is now displayed. `Service` also tries to extract a `message` or `error` field from the JSON error body before falling back to the raw body.

**RMSStore left the record store open on exception.**
the original `saveRecord` and `getRecord` only closed the store in the happy path. exceptions left the handle dangling. v1.1 uses `finally` blocks in both methods.

---

### new features

**`ProfileScreen`** — dedicated screen showing your own posts at `/u/{username}`. previously this was muddled into `HomeScreen`'s endpoint logic.

**`AboutScreen`** — credits screen showing:
- app name and version
- developer: aditya
- [Dash Animation V2](https://www.youtube.com/@dash______animationv2) YouTube channel
- backend URL
reachable from the "about" command on the home screen.

**delete your own posts** — `PostScreen` now shows a "delete" command if the author matches the logged-in username. calls `DELETE /post/{id}` via the new `Service.delete()` method.

**pagination** — `HomeScreen` has a "load more" command that fetches the next page (`?page=N`), appending posts to the existing list instead of replacing them.

**character counter in `CreatePostScreen`** — the TextBox title now shows `post [N/500]` (the field updates on compose; limit dropped from 10 000 to 500 for sensible micro-posts).

**`Service.delete()`** — new HTTP DELETE helper, same pattern as `get()` and `post()`.

**`RMSStore.isLoggedIn()`** — convenience helper for the startup check. `RMSStore.saveLastPostId()` added for future "resume reading" feature.

---

## building

standard J2ME build. requires a WTK (Wireless Toolkit) or equivalent:

```
preverify -classpath $WTK/lib/midpapi20.jar:$WTK/lib/cldcapi11.jar \
    -d preverified src/temporal/*.class

jar cfm temporal.jar MANIFEST.MF -C preverified .
```

MANIFEST.MF minimum:
```
MIDlet-1: temporal, , temporal.Temporal
MIDlet-Name: temporal
MIDlet-Version: 1.1
MIDlet-Vendor: aditya
MicroEdition-Configuration: CLDC-1.1
MicroEdition-Profile: MIDP-2.0
```

---

## api endpoints used

| method | path | auth | description |
|---|---|---|---|
| POST | `/create/token` | none | get JWT (login) |
| POST | `/create/account` | none | register |
| GET | `/posts?page=N` | JWT | global timeline |
| GET | `/u/{username}` | JWT | user's own posts |
| POST | `/create/post` | JWT | create post |
| DELETE | `/post/{id}` | JWT | delete post |

---

## project structure

```
temporal_v1.1/
└── src/
    └── temporal/
        ├── Temporal.java
        ├── LoginScreen.java
        ├── HomeScreen.java
        ├── PostScreen.java
        ├── CreatePostScreen.java
        ├── ProfileScreen.java      ← new
        ├── AboutScreen.java        ← new
        ├── Service.java
        ├── RMSStore.java
        └── JSON.java
```

---

## links

- YouTube: [Dash Animation V2](https://www.youtube.com/@dash______animationv2)
- backend: [temporal.dedomil.workers.dev](https://temporal.dedomil.workers.dev)

---

*temporal v1.1 — J2ME / CLDC 1.1 / MIDP 2.0*
