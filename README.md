
# benchmark-ngs

This tool captures time-series data of CPU, memory, I/O, network, GPU, and GPU memory usage for arbitrary command-line interface programs.

On Linux, the following commands can be used to capture time-series resource usage data:

| Target         | Node-level   | Process-level |
| -------------- | ------------ | ------------- |
| CPU (per core) | `mpstat`     |               |
| CPU (overall)  | `mpstat`     | `pidstat`     |
| Memory         | `free`       | `pidstat`     |
| I/O            | `iostat`     | `pidstat`     |
| Network        | `ifstat`     |               |
| GPU            | `nvidia-smi` | `nvidia-smi`  |
| GPU Memory     | `nvidia-smi` | `nvidia-smi`  |

Node-level measurement programs are launched just before executing the target program and stopped immediately after it finishes. This tool automates that process.

* The current version does not support time-series data collection at the process level.
* Since `benchmark-ngs` is written in Java, the JVM itself can consume around 8GB of memory unnecessarily. This issue can be avoided by compiling the Java program to native code using GraalVM.

  * Reference: [https://www.graalvm.org/latest/reference-manual/native-image/](https://www.graalvm.org/latest/reference-manual/native-image/)

## Installation

Tested environment:

* Ubuntu Linux 24.04
* JDK 23 (GraalVM)
* Apache Maven 3.9.9
* Utility-cli 3.1.0 [https://github.com/oogasawa/Utility-cli](https://github.com/oogasawa/Utility-cli)

For JDK and Maven installation, refer to the following link (SDKMAN! makes it easy):
[https://sc.ddbj.nig.ac.jp/en/guides/software/DevelopmentEnvironment/java/](https://sc.ddbj.nig.ac.jp/en/guides/software/DevelopmentEnvironment/java/)

The tested version details are as follows:

```
$ java -version
openjdk version "23.0.2" 2025-01-21
OpenJDK Runtime Environment GraalVM CE 23.0.2+7.1 (build 23.0.2+7-jvmci-b01)
OpenJDK 64-Bit Server VM GraalVM CE 23.0.2+7.1 (build 23.0.2+7-jvmci-b01, mixed mode, sharing)
```

Since Utility-cli is not registered in Maven Central Repository, you need to manually install it into your local repository (`$HOME/.m2/`) as follows:

```bash
git clone https://github.com/oogasawa/Utility-cli
cd Utility-cli
mvn clean install
```

To install the programs used for measurement, run:

```bash
sudo apt install mpstat free iostat ifstat pidstat
```

To install `nvidia-smi`, follow the standard CUDA installation procedure.

## Build Instructions

```bash
git clone https://github.com/oogasawa/benchmark-ngs
cd benchmark-ngs
mvn clean -Pnative package
```

This will produce the following two files:

* `target/benchmark-ngs` (native binary)
* `target/benchmark-ngs-1.1.0.jar` (fat JAR for JVM with all dependencies included)

## Usage

Running the program without any arguments will display the usage information.

```bash
java -jar benchmark-ngs-1.1.0.jar
```

or

```bash
./benchmark-ngs
```

Example output:

```bash
$ ./benchmark-ngs 

## Usage

java -jar benchmark-ngs-<VERSION>.jar <command> <options>


## Visualization commands

vis:gpu         Draw a graph for a stacked area chart of GPU utilization.
vis:gpuMemory   Generate a CSV file for a stacked area chart of GPU memory utilization.


## benchmark commands

benchmark:processWatch  Continuously monitors process creation and termination events.
benchmark:run           Execute an arbitrary command and collect statistics while it is running.


## format commands

format:gpu          Generate a CSV file for a stacked area chart of GPU utilization.
format:gpuMemory    Generate a CSV file for a stacked area chart of GPU memory utilization.


## parabricks commands

pb:fq2bam_throughput    Extract fq2bam throughput from Parabricks stderr output.
pb:time                 Prints total execution time from each file matching the regular expression.
```

### `benchmark:run` Command

This command executes a target command-line program while automatically launching and stopping the measurement tools.

As an example, here’s how to run `bwa` with measurement:

If `bwa` is not installed, install it with:

```bash
sudo apt install bwa
```

Download the reference genome and sample data:

Reference:

* [https://docs.nvidia.com/clara/parabricks/latest/tutorials/stepbysteptutorials.html](https://docs.nvidia.com/clara/parabricks/latest/tutorials/stepbysteptutorials.html)

```bash
wget -O parabricks_sample.tar.gz "https://s3.amazonaws.com/parabricks.sample/parabricks_sample.tar.gz"
```

Extract the data files:

```bash
tar zxvf parabricks_sample.tar.gz
```

Create a file named `mapping.sh` with the following content:

```bash
cd parabricks_sample/Ref

REF=Homo_sapiens_assembly38.fasta
FASTQ1=../Data/sample_1.fq.gz
FASTQ2=../Data/sample_2.fq.gz
bwa mem -t 8 $REF $FASTQ1 $FASTQ2 > aln.sam
```

Run `bwa` and start the benchmark monitor like this:

```bash
./benchmark-ngs benchmark:run -i 5 -n series01 -- bash mapping.sh
```

This will collect system resource data every 5 seconds. Output files will look like this:

```bash
series01.free.out    series01.iostat.out  series01.pidstat.out
series01.ifstat.out  series01.mpstat.out  series01.program.stdout
```
