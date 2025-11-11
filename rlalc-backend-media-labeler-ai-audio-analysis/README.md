
# rlalc-backend-media-labeler-ai-audio-analysis




## Role

Enrich the media-database, for each [Segment](../README.md#chunk), with [Program](../README.md#program) and [Program-segment](../README.md#segment) information based on AI based audio analysis.

Employs AI techniques (e.g., voice recognition, text analysis) to generate advanced metadata like:
- *segment-real-programid*
- *segment-real-progsegmentid*
- *segment-content-summary*
- *segment-content-transcript'*

These techniques are used in combination with the *segment-source-radioid* and *segment-time-recordingtimestart* attributes provided by the `rlalc-backend-media-capture-service`.


## Execution

Runs in batch/event-based mode when new media is available in the media datastore.
