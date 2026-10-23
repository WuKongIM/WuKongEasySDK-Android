# Streaming demo acceptance

The Android example uses the same persisted base message and public `stream.*`
events as WuKongIM's `demo/streamdemo`. The transport must be the actual SDK;
history is recovery, never a live polling transport.

Failure cases defined before implementation:

- A singleton prevents the user and producer from authenticating independently.
- SEND omits `setting.stream` or changes the caller's `client_msg_no`; appends
  then cannot target the persisted base message. A rejected SENDACK must stop appends.
- Incoming `event` notifications are discarded, malformed data crashes dispatch,
  or events incorrectly produce RECVACK. JSON-string data must be decoded once.
- Snake/camel case response fields lose message identity or stream flags.
- UTF-8 offsets are mistaken for UTF-16 positions, duplicate/overlapping deltas
  repeat text, gaps silently join text, or late events reopen a finished reply.
- Recovery races live events, loses a terminal snapshot, or doubles snapshot text.
- Cancel/failure loses partial content or completion overwrites their terminal state.
- The receiver goes offline; producer must still finish and history must restore it.
- Rotation/recreation loses the session, leaks connections, or duplicates generation.
- SSE is split across UTF-8/network boundaries, contains multiline data, fails,
  ends early, exceeds bounds, or ignores upstream cancellation.
- Credentials, model keys or raw error bodies appear in logs, history or screenshots.

Validation:

1. SDK wire tests with a loopback WebSocket peer, plus deterministic projection
   tests for encoding/recovery edge cases that are hard to schedule on a device.
2. Build the example and run its opt-in instrumentation against a temporary real
   WuKongIM single-node cluster (256 hash slots). Exercise native UI, actual SEND,
   EVENT, cancel/error, receiver reconnect, history, activity recreation and a local
   SSE fixture. Export `report.json`, screenshots and a bounded server log.
3. No paid model or production server is required. API, model key and session
   credentials are not written into the exported report.

## Validation record — 2026-10-23

- Android source baseline: `61ae6dc6d0077b15e47cda1fd530296b97a06a7a` (v1.0.5).
- Reference JS SDK: `b6d0bbe822b9c5b6f95a10d55b593d30184414f6` (easyjssdk 2.0.5).
- Real server: WuKongIM `a804f4a57`, freshly built from the operator's checkout.
- Device: Android 14 / API 34 ARM64 ATD emulator, JDK 17, Gradle 8.4.
- SDK and example checks: 25 tests passed, debug app and instrumentation APKs built;
  Android lint passed.
- Real-server native acceptance passed: live UTF-8 deltas, complete/cancel/error,
  offline generation, recovery, recreation, recovery during generation, local SSE
  completion, model error, truncated SSE, upstream cancellation, multi-turn context,
  and credential exclusion from transcript/logs. Exactly three history reads (initial
  connect and two receiver reconnects), no eventsync reads.
- Repeatable runner: `example/tools/stream_acceptance.py`. The output directory
  contains a machine-readable report, source revision/dirty state, device API,
  fixture request roles, screenshots, instrumentation output and server log.
- Only API 34 was device-tested; this is not certification of the full Android API
  21+ support matrix. No real external model provider or paid model request was used.
