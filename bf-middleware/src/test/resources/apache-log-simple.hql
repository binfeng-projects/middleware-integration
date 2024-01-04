
ADD JAR ${hiveconf:hiveContribJar};

DROP TABLE apachelog;

-- 如果报错 cannot recognize input near 'time' xxx，看下是否用了关键字，如果是，用符号包裹起来`time`
CREATE TABLE apachelog(remoteHost STRING, remoteLogname STRING, userName STRING, `time` STRING, method STRING, uri STRING, proto STRING, status STRING, bytes STRING, referer STRING,  userAgent STRING) ROW FORMAT SERDE 'org.apache.hadoop.hive.contrib.serde2.RegexSerDe' WITH SERDEPROPERTIES (  "input.regex" = "^([^ ]*) +([^ ]*) +([^ ]*) +\\[([^]]*)\\] +\\\"([^ ]*) ([^ ]*) ([^ ]*)\\\" ([^ ]*) ([^ ]*) (?:\\\"-\\\")*\\\"(.*)\\\" (.*)$", "output.format.string" = "%1$s %2$s %3$s %4$s %5$s %6$s %7$s %8$s %9$s %10$s %11$s" ) STORED AS TEXTFILE;

-- If using variables and executing from HiveTemplate (vs HiveRunner), need to put quotes around the variable name.
-- -- 如果报错 mismatched input '/' expecting StringLiteral near 'INPATH' in load statement，需要用引号把变量括起来
LOAD DATA INPATH '${hiveconf:localInPath}' INTO TABLE apachelog;

-- determine popular URLs
INSERT OVERWRITE DIRECTORY '/user/hive/output/uri_hits' SELECT a.uri, "\t", COUNT(*) FROM apachelog a GROUP BY a.uri ORDER BY uri;
