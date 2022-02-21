# Rekindle 🔥

A toolkit for building Apache Spark event log analyzers. Rekindle your old event logs to give them a new spark.

## (non-committal) Project goals

The vision is to build a service that continuously tails HDFS/S3 Spark event log dir and exposes
metrics derived from them. This would allow to observe and analyze long term trends
(duration, resource utilization, size of inputs and outputs) of regular (daily/hourly/...) Spark jobs.
Alerting rules could be implemented to notify about change in trends. A specific very example of an use case
that I have on mind is to fire an alert if the duration of a regular daily job has risen by (let's say) 30% in the last month.

Feature (wish)list

- [ ] CLI that read event logs from stdin/local file system and outputs stream of metrics and messages.
      This should serve mostly as a showcase for potential users - to have a taste
      of the capabilities without having to deploy any services
- [ ] Persistent storage (with multiple backends - embedded is a must for test deploys)
- [ ] API for querying
- [ ] UI to visualize metrics
- [ ] Ways to decouple ingestion and querying and horizontal scalability of both (inspired by Thanos)
- [ ] DSL for defining trigger on metrics

## Motivation

This project is the result of my frustration with monitoring Spark batch jobs and their performance over time.

I have seen attempts to monitor Spark jobs using Prometheus and/or other time series based solutions.
While this works OK for monitoring executor metrics, I did not encounter a satisfactory solution
for data about individual job stages and job failure/success rates.

Working on a large system with many ETL jobs, I experienced continuous deterioration caused
by changes in data volumes and cruft slowly accrued with small business rules changes.
Many of these regressions could be usually quickly fixed with small adjustment of job parameters.
The problem is that we were usually made aware of the regressions only when they started
to cause problems and data processing delays ("Where are there no data in Hive by 8  o'clock?"
"Because the job that used to take 2 hours now takes 5").

I would also like to have a system that could give suggestions *how* to optimize jobs.
Spark veterans can gather insight about jobs by pouring over Spark history server tables and graphs.
However, that takes time and a lot of experience.
I would like to synthesize this knowledge into rules that run over the same data
that power the history server - application event logs.

Projects that inspired rekindle:

- The venerable Spark history server
- [Dr. Elephant](https://github.com/linkedin/dr-elephant), which I never actually managed to get working with Hadoop and Spark versions that I have been using.
- [sparklens](https://github.com/qubole/sparklens) (I do plan to integrate sparklens reports into rekindle)
- [sparkMeasure](https://github.com/LucaCanali/sparkMeasure)

## A note on Apache Spark version and compatibility

No part of Rekindle is meant to run as a part of your Spark application or cluster.
That means no new jars added to classpath and no non-standard configuration.

It also means that you don't have to care about the specific version of Spark in `built.sbt`,
only the compatibility of Spark event logs.
AFAIK the backwards compatibility is pretty good and logs produces by Spark 2.x app are still
compatible with Spark 3.x event logs. The only issue could be that older Spark version may
not produce some events or they could be missing some info, so some metrics may not be produced
for older Spark apps.

My plan is to explore the compatibility in more detail and setup an integration test suite
for event logs produced by a range of Spark versions >= 2.x.
