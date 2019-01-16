# java-tools
Some small usefull java tools I wrote for various tasks.
Currently only the log4jparser available

# Log4jParser
The log4jparser can be used to extract exception information from big log files filtering by log level and optional custom regular expression (tested with max 1GB, but probably will work also for bigger log files). The tool may also group, count and order the exceptions by certain criteria (currently only count and first date match supported). Same or similar exceptions are grouped by finding similar first log line or by comparing the exception body. The first line may differs as it contains sometimes information where the exception happened (e.g. different resources, different paths), but the exception body is mostly the same, so such exception will be grouped together.

## Requirements
 - java 8
 - write access to the directory where the file is analyzed (used to create temporary files while sorting big files)

## Usage
### Download and build
Clone from git and build with maven:

```bash
git clone https://github.com/d33t/java-tools.git
cd java-tools/
mvn clean compile assembly:single install
```
### First run and usage
Run from the project directory:

```bash
java -jar target/java-tools-2.0-SNAPSHOT-jar-with-dependencies.jar <options>

usage: log4jparser
 -d,--dateFormat <arg>   (optional) Specify the log format of the log
                         entries. Defaults to: dd.MM.yyyy HH:mm:ss.SSSS
 -i,--inputFile <arg>    Absolute or relative to the current directory
                         path to the logfile (text or zip)
 -l,--loglevel <arg>     A valid log4J log level: [FATAL, ERROR, WARN,
                         INFO, DEBUG, TRACE]. Multiple values can be
                         separated by comma or space.
 -o,--outputFile <arg>   (optional) Absolute or relative to the current
                         directory path to the output file. If omitted the
                         standard output is used.
 -p,--pattern <arg>      (optional) Pattern to match
 -s,--sort <arg>         (optional) Sort either by date or unique count.
                         This option is only used when 'unique' flag is
                         set. Default to date.
 -u,--unique             (optional) Unique lines with occurrence count
 ```
 
## Examples
### Set alias in your bash profile
If you don't want to remember or write so much every time you want to run the tool, just create a suitable alias. You can even set there some java parameter (for example more memory).

File: ~/.profile

```bash
log4jparser_current=~/.m2/repository/net/demonsteam/tools/java-tools/2.0-SNAPSHOT/java-tools-2.0-SNAPSHOT-jar-with-dependencies.jar
alias log4jparser="java -Xms512m -Xmx1g -jar $log4jparser_current"
```

### Group, Count, Sort
Extract, group and sort all `ERROR,FATAL` log level entries by there occurrence in the log file

```bash
log4jparser -l error,fatal -s count --unique -i /tmp/mylog.log -o output.log
```

# License
java-tools is distributed under MIT licence, so feel free to do whenever you want with the code.