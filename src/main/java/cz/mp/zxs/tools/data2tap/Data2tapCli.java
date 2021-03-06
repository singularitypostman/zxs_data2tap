/*
 * Data2tapCli.java
 *
 *  created: 12.3.2019
 *  charset: UTF-8
 */

package cz.mp.zxs.tools.data2tap;

import static cz.mp.zxs.tools.data2tap.Version.VERSION;
import cz.mp.zxs.tools.data2tap.gui.MainFrame;
import cz.mp.utils.FileUtils;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static cz.mp.utils.TextSource.*;

/**
 * CLI (Command Line Interface) pro {@code Data2tap}.
 * <p>
 * viz definici parametrů v kódu metody {@linkplain #createOptions() }. 
 * Stručně:
 * <tt><pre>
 * základní info (mají nejvyšší prioritu)
 *      --help
 *      --version
 * GUI (má přednost před volbami pro CLI ovládání)
 *      --gui
 * parametry pro ovládání z CLI
 *      -s title
 *      -a adress
 *          - povinný
 *      -i input_binary_file 
 *          - povinný
 *      -o out_tap_file
 * </pre></tt>
 * @author Martin Pokorný
 */
public class Data2tapCli {
    private static final Logger log = LoggerFactory.getLogger(Data2tapCli.class);

    private Options options = new Options();
    private HelpFormatter helpFormatter = new HelpFormatter();

    // hodnoty z parametrů programu
    private boolean optHelp = false;
    private boolean optVersion = false;
    private boolean optGui = false;
    private String optInputDataFileName = null;
    private String optOutTapFileName = null; 
    private String optAdress = null;
    private static final String DEFAULT_NAME = "";
    private String optName = DEFAULT_NAME;      // ("", ne null)
            
    private static final ZxModel DEFAULT_ZXMODEL = ZxModelSpectrum48k.get();
    private ZxModel zxModel = DEFAULT_ZXMODEL;
    
    public static final int RESULT_OK = 0;
    public static final int RESULT_ERR_GENERAL = 1;
    public static final int RESULT_ERR_OPTS = 2;
    public static final int RESULT_ERR_DATA2TAP = 3;
    
    /** */
    public Data2tapCli() {
        createOptions();
    }    
    
    /**
     * 
     */
    private void printHelp() {
        log.info("");
        pout(getLocText("cli.help.intro"));
        pout(getLocText("cli.help.usage") + ":");
        pout("  java -jar zxs_data2tap.jar [options...]");
        pout(getLocText("cli.help.options") + ":");
        StringWriter sw = new StringWriter();
        helpFormatter.printOptions(new PrintWriter(sw), 79, options, 2, 2);
        poutNoEol(sw.toString());
        pout(getLocText("cli.help.examples") + ":");
        pout("  java -jar zxs_data2tap.jar --gui");
        pout("  java -jar zxs_data2tap.jar -i img.scr -a 0x4000 -s screen -o img.tap");
    }
    
    /**
     * 
     */
    private void printVersion() {
        log.info("");
        pout(VERSION); 
    }
    
    private static void pout(String text) {
        System.out.println(text);
    }

    private static void poutNoEol(String text) {
        System.out.print(text);
    }

    // private static void perr(String text) {
    //     System.err.println(text);
    // }
    
    /**
     * 
     * @param msg
     * @param errCode 
     */
    private static void exitWithError(String msg, int errCode) {
        log.error("Error: " + msg);
        pout(getLocText("error") + ": " + msg);
        System.exit(errCode);
    }
    
    /**
     * 
     * @param ex
     * @param errCode 
     */
    private static void exitWithError(Exception ex, int errCode) {
        log.error("Error: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
        pout(getLocText("error") + ": " + ex.getClass().getName() + ": " + ex.getMessage());
        System.exit(errCode);
    }
    
    /**
     * 
     */
    private void createOptions() {
        Option help = Option.builder("h")
                .longOpt("help")
                .hasArg(false)
                .required(false)
                .desc(getLocText("cli.help.opt.help"))
                .build();
        options.addOption(help);
        
        Option version = Option.builder("v")
                .longOpt("version")
                .hasArg(false)
                .required(false)
                .desc(getLocText("cli.help.opt.version"))
                .build();
        options.addOption(version);

        Option gui = Option.builder()
                .longOpt("gui")
                .hasArg(false)
                .required(false)
                .desc(getLocText("cli.help.opt.gui"))
                .build();
        options.addOption(gui);

        Option inFileName = Option.builder("i")
                .hasArg(true)
                .required(false)
                .desc(getLocText("cli.help.opt.i"))
                .build();
        options.addOption(inFileName);

        Option outFileName = Option.builder("o")
                .hasArg(true)
                .required(false)
                .desc(getLocText("cli.help.opt.o"))
                .build();
        options.addOption(outFileName);

        Option adress = Option.builder("a")
                .longOpt("address")
                .hasArg(true)
                .required(false)
                .desc(getLocText("cli.help.opt.address"))
                .build();
        options.addOption(adress);
        
        Option title = Option.builder("s")
                .longOpt("name")
                .hasArg(true)
                .required(false)
                .desc(getLocText("cli.help.opt.name"))
                .build();
        options.addOption(title);
    }
    
    /**
     * 
     * @param args 
     */
    private void parseArgs(String[] args) {
        log.info("");
        try {
            CommandLineParser parser = new DefaultParser();
            
            CommandLine commandLine = parser.parse(options, args);
                       
            if (commandLine.getOptions().length == 0) {     // žádné parametry
                printHelp();
                exitWithError("missing program options", RESULT_ERR_OPTS);
            }
            
            if (commandLine.hasOption("h")) {
                log.info("--help");
                optHelp = true;
            }
            if (commandLine.hasOption("v")) {
                log.info("--version");
                optVersion = true;
            }
            if (commandLine.hasOption("gui")) {
                log.info("--gui");
                optGui = true;
            }
            if (commandLine.hasOption("i")) {
                optInputDataFileName = commandLine.getOptionValue("i");
                log.info("-i = " + optInputDataFileName);
            } 
            if (commandLine.hasOption("o")) {
                optOutTapFileName = commandLine.getOptionValue("o");
                log.info("-o = " + optOutTapFileName);
            }
            if (commandLine.hasOption("a")) {
                optAdress = commandLine.getOptionValue("a");
                log.info("-a = " + optAdress);
            } 
            if (commandLine.hasOption("s")) {
                optName = commandLine.getOptionValue("s");
                log.info("-s = \"" + optName + "\"");
            }             
        }
        catch (ParseException pex) {
            exitWithError(pex, RESULT_ERR_OPTS);
        }
    }

    /**
     * Obslouží parametry {@code --help}, {@code --version}.
     */
    private void executeInfoOptsAndExit() {
        if (optHelp) {
            printHelp();
        }
        if (optVersion) {
            printVersion();
        }        
    }
    
    /**
     * Obslouží parametr {@code --gui}.
     */
    private void executeGuiOpt() {
        log.info("");
        
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                log.info("create and show GUI");
                MainFrame.getInstance().setVisible(true);
            }
        });        
    }
    
    /**
     * Obslouží parametry pro ovládání programu z CLI.
     * <p>
     * viz též {@linkplain MainFrame#createTapFileWithDialogs()}.
     */
    private void executeCliOpts() {
        log.info("");
        
        // 1. --- získání hodnot a dat z parametrů + předvalidace
                
        if (optInputDataFileName == null) {
            exitWithError(getLocText("cli.err.missing_i"), RESULT_ERR_OPTS);          
        }
        if (optOutTapFileName == null) {
            exitWithError(getLocText("cli.err.missing_o"), RESULT_ERR_OPTS);          
        }
        if (optName.length() > TapHeader.NAME_LEN) {
            exitWithError(getLocText("cli.err.name_too_long"), RESULT_ERR_OPTS);                      
        }
        
        // - optAdress --> address
        int address = -1;
        try {
            if (optAdress == null) {
                exitWithError(getLocText("cli.err.missing_a"), RESULT_ERR_OPTS);
            }
            address = MemoryAddress.addressToInt(optAdress);
            log.debug("address = " + address);
        } catch (NumberFormatException ex) {
            exitWithError(getLocText("cli.err.address_not_a_number"), RESULT_ERR_OPTS);
        }

        // - optInputDataFileName --> byte[]
        File inFile = new File(optInputDataFileName);
        if (!inFile.exists() || inFile.isDirectory()) {
            exitWithError(getLocText("cli.err.i_file_not_found", optInputDataFileName), RESULT_ERR_OPTS);
        }
        
        byte[] inputFileContent = new byte[]{};
        try {
            inputFileContent = Files.readAllBytes(inFile.toPath());
            log.debug("inputFileContent.length = " + inputFileContent.length);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
            exitWithError(optInputDataFileName + " : " + ex.getMessage(), 
                    RESULT_ERR_OPTS);
        }

        if (inputFileContent.length == 0) {
            exitWithError(getLocText("cli.err.i_file_empty", optInputDataFileName), RESULT_ERR_OPTS);
        }
        
        // - optOutTapFileName -- kontrola přípany, pokud soubor existuje
        String outFileExt = FileUtils.getFileExtension(optOutTapFileName);
        log.debug("outFileExt = \"" + outFileExt + "\"");
        File outTapFile = new File(optOutTapFileName);
        if (! outFileExt.toLowerCase().equals("tap")
                && outTapFile.exists() && outTapFile.isFile()) {
            exitWithError(getLocText("cli.err.o_overwriting_non_tap"), RESULT_ERR_OPTS);                
        }
         
        // -------------
        // 2. --- vytvoření a zápis TAP souboru

        Data2tap data2tap = new Data2tap();
        log.debug("set data to Data2tap");
        data2tap.setModel(zxModel);
        data2tap.setTapBlockType(TapBlockType.BINARY_DATA);
        data2tap.setName(optName);
        data2tap.setAddress(address);
        data2tap.setRawData(inputFileContent);
        data2tap.setOutTapFile(outTapFile);

        try {
            log.debug("data2tap.execute!");
            data2tap.execute();
            
            if (outTapFile.exists() && outTapFile.isFile()) {
                log.info(outTapFile.getName() + " successfully created");
                log.info("Data size = " + inputFileContent.length + " B");
                log.info("File size = " + outTapFile.length() + " B");                
                pout(getLocText("cli.ok.outfile_created", outTapFile.getName()));
                pout("Data size = " + inputFileContent.length + " B");
                pout("File size = " + outTapFile.length() + " B");
            }
            else {      // (nemělo by nastat)
                log.warn(outTapFile.getName() + " doesn't exist");
                pout("Unexpected failure. See log file for more details.");
            }
        } catch (InvalidDataException | IOException ex) {
            exitWithError(ex, RESULT_ERR_DATA2TAP);
        } catch (Exception ex) {
            exitWithError(ex, RESULT_ERR_GENERAL);
        }
        
        log.debug("end");
    }
    
    /**
     * 
     * @param args 
     */
    public void executeWithArgs(String[] args) {
        parseArgs(args);

        if (optHelp || optVersion) {    // tyto mají přednost
            executeInfoOptsAndExit();
            System.exit(RESULT_OK);
        }
        else if (optGui) {   // --gui má přednost před volbami pro CLI rozhraní
            executeGuiOpt();
        }
        else {
            executeCliOpts();
            System.exit(RESULT_OK);
        }
    }
    
}   // Data2tapCli.java