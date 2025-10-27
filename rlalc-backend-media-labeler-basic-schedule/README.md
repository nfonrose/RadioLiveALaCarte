
# rlalc-backend-media-labeler-basic-schedule




## Role

Enrich the media-database, for each [Segment](../README.md#segment-rlac), with [Program](../README.md#program) and [Program-segment](../README.md#program-segment-aka-segment-gmfm) information based on theoritical radio schedules.

Use basic information such as *segment-source-radioid* and *segment-time-recordingtimestart* combined with Radio program schedules to add metadata like:
- *segment-approx-programid*
- *segment-approx-programsegmentid*


## Execution

Runs in batch/event-based mode when new media is available in the media datastore.
