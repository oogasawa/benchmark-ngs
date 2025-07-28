package com.github.oogasawa.benchmark.ngs.parabricks;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import com.github.oogasawa.benchmark.ngs.parabricks.fq2bam.BatchTimeToTsv;
import com.github.oogasawa.benchmark.ngs.parabricks.fq2bam.Fq2BamThroughput;
import com.github.oogasawa.utility.cli.CommandRepository;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;


public class ParabricksCommands {

    private static final Logger logger = Logger.getLogger(ParabricksCommands.class.getName());
    /**
     * The command repository used to register commands.
     */
    CommandRepository cmdRepos = null;
    
    /**
     * Registers all JAR-related commands in the given command repository.
     * 
     * @param cmds The command repository to register commands with.
     */
    public void setupCommands(CommandRepository cmds) {
        this.cmdRepos = cmds;

        pbFq2bamThroughputCommand();
        pbTimeCommand();
        
    }


    /**
     * Registers the "pb:time" command, which lists the total execution time from each Parabricks stderr file
     * whose relative path matches the given regular expression.
     */
    public void pbTimeCommand() {
        Options opts = new Options();

        opts.addOption(Option.builder("f")
                       .longOpt("files")
                       .hasArg(true)
                       .argName("REGEX")
                       .desc("A regular expression that matches relative paths of files with Parabricks stderr messages.")
                       .required(true)
                       .build());

        this.cmdRepos.addCommand(
                                 "parabricks commands",
                                 "pb:time",
                                 opts,
                                 "Prints total execution time from each file matching the regular expression.",
                                 (CommandLine cl) -> {
                                     String regexStr = cl.getOptionValue("files");
                                     try {
                                         BatchTimeToTsv.parse(regexStr);
                                     } catch (PatternSyntaxException e) {
                                         logger.log(Level.SEVERE, String.format("Invalid regular expression: %s", e.getMessage()));
                                         System.exit(2);
                                     } catch (Throwable t) {
                                         logger.log(Level.SEVERE, String.format("UNRECOVERABLE: Application failed: %s", t.getMessage()), t);
                                         System.exit(1);
                                     }
                                 });
    }



    

    public void pbFq2bamThroughputCommand() {
        Options opts = new Options();

        opts.addOption(Option.builder("i")
                       .longOpt("infile")
                       .hasArg(true)
                       .argName("FILE")
                       .desc("A Parabricks fq2bam stderr file.")
                       .required(true)
                       .build());

        opts.addOption(Option.builder("o")
                       .longOpt("outfile")
                       .hasArg(true)
                       .argName("FILE")
                       .desc("An output CSV file. If omitted, a file named <infile basename>.throughput.csv will be created.")
                       .required(false)
                       .build());

        this.cmdRepos.addCommand(
                                 "parabricks commands",
                                 "pb:fq2bam_throughput",
                                 opts,
                                 "Extract fq2bam throughput from Parabricks stderr output.",
                                 (CommandLine cl) -> {
                                     String inputPath = cl.getOptionValue("infile");
                                     String outputPath = cl.getOptionValue("outfile");

                                     if (outputPath == null || outputPath.isBlank()) {
                                         Path input = Paths.get(inputPath);
                                         String filename = input.getFileName().toString();
                                         int dotIndex = filename.lastIndexOf('.');
                                         String baseName = (dotIndex == -1) ? filename : filename.substring(0, dotIndex);

                                         Path parent = input.getParent();
                                         Path output = (parent != null)
                                             ? parent.resolve(baseName + ".throughput.csv")
                                             : Paths.get(baseName + ".throughput.csv");

                                         outputPath = output.toString();
                                         logger.info(outputPath);
                                     }



                                     try {
                                         Fq2BamThroughput.parseAndExport(Paths.get(inputPath), Paths.get(outputPath));
                                     } catch (IOException e) {
                                         logger.log(Level.SEVERE, String.format("I/O error: %s", e.getMessage()), e);
                                         System.exit(2);
                                     } catch (Throwable t) {
                                         logger.log(Level.SEVERE, String.format("UNRECOVERABLE: Application failed: %s", t.getMessage()), t);
                                         System.exit(1);
                                     }
                                 });
    }




    
}
