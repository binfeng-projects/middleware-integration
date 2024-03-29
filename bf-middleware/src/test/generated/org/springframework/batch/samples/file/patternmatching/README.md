### Pattern Matching Sample

## About

The goal is to demonstrate how to handle a more complex file input
format, where a record meant for processing includes nested records
and spans multiple lines.

The input source is a file with multiline records:

```
HEA;0013100345;2007-02-15
NCU;Smith;Peter;;T;20014539;F
BAD;;Oak Street 31/A;;Small Town;00235;IL;US
SAD;Smith, Elizabeth;Elm Street 17;;Some City;30011;FL;United States
BIN;VISA;VISA-12345678903
LIT;1044391041;37.49;0;0;4.99;2.99;1;45.47
LIT;2134776319;221.99;5;0;7.99;2.99;1;221.87
SIN;UPS;EXP;DELIVER ONLY ON WEEKDAYS
FOT;2;2;267.34
HEA;0013100346;2007-02-15
BCU;Acme Factory of England;72155919;T
BAD;;St. Andrews Road 31;;London;55342;;UK
BIN;AMEX;AMEX-72345678903
LIT;1044319101;1070.50;5;0;7.99;2.99;12;12335.46
LIT;2134727219;21.79;5;0;7.99;2.99;12;380.17
LIT;1044339301;79.95;0;5.5;4.99;2.99;4;329.72
LIT;2134747319;55.29;10;0;7.99;2.99;6;364.45
LIT;1044359501;339.99;10;0;7.99;2.99;2;633.94
SIN;FEDX;AMS;
FOT;5;36;14043.74
```

`OrderItemReader` is an example of a non-default programmatic
item reader. It reads input until it detects that the multiline
record has finished and encapsulates the record in a single domain
object.

The output target is a file with multiline records.  The concrete
`ItemWriter` passes the object to a an injected 'delegate
writer' which in this case writes the output to a file.  The writer
in this case demonstrates how to write multiline output using a
custom aggregator transformer.

## Run the sample

You can run the sample from the command line as following:

```
$>cd spring-batch-samples
$>../mvnw -Dtest=PatternMatchingJobFunctionalTests#testJobLaunch test
```

