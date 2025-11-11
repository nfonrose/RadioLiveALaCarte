
# rlalc-backend-media-labeler-basic-schedule




## Role

Enrich the chunks stored in the media-database with [programId](../README.md#program) and [segmentId](../README.md#segment) metadata information,
computed using "theoretical radio schedules" and "recording times".

Use basic information such as *chunk-recordingid* and *chunk-time-recordingtimestart* combined with Radio program schedules to compute metadata values like:
- *chunk-approx-programid*
- *chunk-approx-segmentid*


## Execution

Runs in batch/event-based mode when new media is available in the media datastore.
