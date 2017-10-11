/*
 * MainFrame.java
 *
 *  created: 4.10.2017
 *  charset: UTF-8
 */

package cz.mp.zxs.tools.data2tap.gui;

import cz.mp.zxs.tools.data2tap.utils.GuiUtils;
import cz.mp.zxs.tools.data2tap.InvalidDataException;
import cz.mp.zxs.tools.data2tap.TapBlockType;
import cz.mp.zxs.tools.data2tap.TapBody;
import cz.mp.zxs.tools.data2tap.TapHeader;
import cz.mp.zxs.tools.data2tap.Version;
import cz.mp.zxs.tools.data2tap.ZxsConstants;
import static cz.mp.zxs.tools.data2tap.ZxsConstants.ADR_MAX__48K;
import cz.mp.zxs.tools.data2tap.gui.component.LabelBold;
import cz.mp.zxs.tools.data2tap.utils.FileUtils;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.ArrayList;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.text.MaskFormatter;
import javax.swing.undo.UndoManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Hlavní okno nástroje {@code data2tap} pro ZX Spectrum.
 * 
 * @author Martin Pokorný
 */
public class MainFrame {
    private static final Logger log = LoggerFactory.getLogger(MainFrame.class);
    static {
        log.debug("start initialisation");
        
        UIManager.put("swing.boldMetal", false);
        UIManager.put("swing.aatext", true);
    }

    private static final String TITLE = "zxs_data2tap";   
    private JFrame frame;

    private static MainFrame instance = null;

    private LabelBold nameLabel = new LabelBold("Name");
    private JFormattedTextField nameField = new JFormattedTextField();

    private LabelBold typeLabel = new LabelBold("Type");
    private JComboBox typeCombo = new JComboBox();

    private LabelBold adressLabel = new LabelBold("Adress");
    private JComboBox adressCombo = new JComboBox();

    private LabelBold dataLabel = new LabelBold("Data");

    private JEditorPane dataTextArea = new JEditorPane();
    private JScrollPane dataScrollPane = new JScrollPane(dataTextArea);
    private UndoManager dataTextAreaUndoManager = new UndoManager();

    private JLabel dataRadixLabel = new JLabel("Radix");
    private JComboBox dataRadixCombo = new JComboBox();
            
    private JButton loadDataBtn = new JButton("Load binary file");
    
    private Font monoBiggerFont = new Font("Monospaced", 
            Font.PLAIN, dataTextArea.getFont().getSize()+2);
            
    private JButton createTapBtn = new JButton("Create TAP file");
   
    private JTextField aboutSelectableLabel = new JTextField("");

    
    private JFileChooser loadBinaryFileChooser = null;

    private JFileChooser saveTapFileChooser = null;
    
    
    // -----
    /** */
    private MainFrame() {
        super();
        this.frame = new JFrame(TITLE);

        try {
            initComponents();
            initLayout();
            initEventHandlers();
            initFrame();
            nameField.requestFocusInWindow();
            log.debug("init done");
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    };


    private void initFrame() {
        log.debug("");

        ArrayList<Image> icons = new ArrayList<Image>();
        //icons.add(Images.getImage(Images.ICON_16).getImage());
        icons.add(Images.getImage(Images.ICON_32).getImage());
        //icons.add(Images.getImage(Images.ICON_64).getImage());
        frame.setIconImages(icons);

        initFrameSizeAndPosition();

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    private void initFrameSizeAndPosition() {
        log.debug("");
        frame.pack();
        frame.setMinimumSize(new Dimension(frame.getWidth(), frame.getHeight()));
        frame.setSize(new Dimension(frame.getWidth(), frame.getHeight()+130));

        frame.setLocationRelativeTo(null);
    }

    private void initComponents() {
        log.debug("");

        typeCombo.setEditable(false);
        typeCombo.addItem(TapBlockType.CODE_OR_SCREEN);    // Nyní JEN TapBlockType.CODE_OR_SCREEN !
        
        dataTextArea.setFont(monoBiggerFont);
        dataScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        adressCombo.addItem(new ValueDescriptionItem("",""));
        adressCombo.addItem(new ValueDescriptionItem("0x4000","SCREEN$"));
        adressCombo.addItem(new ValueDescriptionItem("0x5800","SCREEN$ Attribs"));
        adressCombo.addItem(new ValueDescriptionItem("0x7F58","UDG for ZXS 16k"));
        adressCombo.addItem(new ValueDescriptionItem("0xFF58","UDG for ZXS 48k"));
        adressCombo.setSelectedIndex(0);
        adressCombo.setToolTipText("0x4000 (16384) \u2013 0xFFFF (65535)");
        adressCombo.setEditable(true);
        adressCombo.setRenderer(new ValueDescriptionCellRenderer());
                
        try {
            MaskFormatter formatter = new MaskFormatter("**********");
            formatter.setValidCharacters("ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                    + "abcdefghijklmnopqrstuvwxyz"
                    + " _-+.,;0123456789");
            nameField = new JFormattedTextField(formatter);
        } catch (ParseException ex) {
            log.error(ex.getMessage(),ex);
            nameField = new JFormattedTextField();
        }
        nameField.setToolTipText("Max 10 characters");
        
        dataRadixCombo.addItem(Radix.DECIMAL);
        dataRadixCombo.addItem(Radix.HEXADECIMAL);
        dataRadixCombo.setSelectedItem(Radix.DECIMAL);

        initAbout();
        
        dataTextArea.getDocument().addUndoableEditListener(dataTextAreaUndoManager);
    }
    
    private void initAbout() {
        log.debug("");
        // (c) \u00A9 2017 Martin Pokorn\u00FD
        aboutSelectableLabel.setText(" \u00A9 2017 Martin Pokorn\u00FD"
                + "   MartinPokorny.czech@gmail.com"
                + "   Version:" + Version.VERSION_SPEC + " ");
        aboutSelectableLabel.setOpaque(true);
        aboutSelectableLabel.setEditable(false);
        aboutSelectableLabel.setBackground(null);
        aboutSelectableLabel.setBorder(null);
//        aboutSelectableLabel.setBorder(BorderFactory.createEmptyBorder());
        aboutSelectableLabel.setMargin(new Insets(6, 3, 3, 3));
    }

    private void initLayout() {
        log.debug("");
        frame.setLayout (new GridBagLayout());

        Insets ins5555 = new Insets(5, 5, 5, 5);
        Insets ins5505 = new Insets(5, 5, 0, 5);
        Insets ins0000 = new Insets(0, 0, 0, 0);

        int r=0;
        Container c = frame.getContentPane();

        c.add(GuiUtils.createMinWidthFoobar(130,150), new GridBagConstraints(1,r,1,1,1.0,0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, ins0000, 0,0));
        c.add(GuiUtils.createMinWidthFoobar(130,150), new GridBagConstraints(2,r,1,1,1.0,0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, ins0000, 0,0));
        r++;

        c.add(typeLabel, new GridBagConstraints(0,r,1,1,0.0,0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, ins5505, 0,0));
        c.add(typeCombo, new GridBagConstraints(1,r,1,1,0.0,0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, ins5505, 0,0));
        r++;
        
        c.add(nameLabel, new GridBagConstraints(0,r,1,1,0.0,0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, ins5505, 0,0));
        c.add(nameField, new GridBagConstraints(1,r,1,1,0.0,0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, ins5505, 0,0));
        r++;

        c.add(adressLabel, new GridBagConstraints(0,r,1,1,0.0,0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, ins5505, 0,0));
        c.add(adressCombo, new GridBagConstraints(1,r,1,1,0.0,0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, ins5505, 0,0));
        r++;

        // ----
        c.add(new JSeparator(JSeparator.HORIZONTAL), new GridBagConstraints(0,r,11,1,1.0,0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
                new Insets(10, 5, 5, 5), 0, 0));
        r++;

        c.add(dataLabel, new GridBagConstraints(0,r,1,1,0.0,0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, ins5505, 0,0));
        r++;
        
        c.add(dataRadixLabel, new GridBagConstraints(0,r,1,1,0.0,0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, ins5505, 0,0));
        c.add(dataRadixCombo, new GridBagConstraints(1,r,1,1,0.0,0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, ins5505, 0,0));
                
        c.add(loadDataBtn, new GridBagConstraints(4,r,1,1,0.0,0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, ins5505, 0,0));
        r++;
                
        c.add(dataScrollPane, new GridBagConstraints(0,r,5,5,1.0,1.0,
                GridBagConstraints.WEST, GridBagConstraints.BOTH, ins5555, 0, 0));
        // "vata" - vertikální mezera s min. výškou
        c.add(new JLabel(), new GridBagConstraints(10,r,1,1,0.0,1.0,
                GridBagConstraints.WEST, GridBagConstraints.VERTICAL, ins0000, 0, 150));

        r+=5;        
        
        c.add(createTapBtn, new GridBagConstraints(0,r,5,1,1.0,0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(10, 5, 10, 5), 0,0));
        r++;


        c.add(new JSeparator(JSeparator.HORIZONTAL), new GridBagConstraints(0,r,10,1,1.0,0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(10, 0, 0, 0), 0,0));
        r++;

        c.add(aboutSelectableLabel, new GridBagConstraints(0,r,10,1,1.0,0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, ins5555, 0,0));
        
        // ----
        
        // aby měl nameField stejnou velikost (jde mi hlavně o výšku) jako adressCombo
        //nameField.setMinimumSize(adressCombo.getMinimumSize());
        nameField.setPreferredSize(adressCombo.getPreferredSize());
        //nameField.setSize(adressCombo.getSize());
    }

    /** */
    private void initEventHandlers() {
        log.debug("");

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                log.info("close frame!");
                System.exit(0);
            }
        });
        
        dataTextArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Z) {
                    if (dataTextAreaUndoManager.canUndo()) {
                        dataTextAreaUndoManager.undo();
                    }
                }
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Y) {
                    if (dataTextAreaUndoManager.canRedo()) {
                        dataTextAreaUndoManager.redo();
                    }
                }
            }
        });        
        
        loadDataBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                log.debug("(loadDataBtn click)");
                loadBinaryFileWithDialogs();                
            }
        });
        
        dataRadixCombo.addItemListener(new ChangeDataRadixItemListener());
        
        createTapBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                log.debug("(createTapBtn click)");
                createTapFileWithDialogs();
            }
        });
    }

    /**
     *
     * @return
     */
    public static MainFrame getInstance() {
        if (instance == null) {
            instance = new MainFrame();
        }
        return instance;
    }

    public void setVisible(boolean b) {
        log.info("" + b);
        frame.setVisible(b);
    }

    /**
     * 
     * @param origRadix
     * @param newRadix 
     * @throws InvalidDataException
     * @throws IllegalArgumentException
     * @see #parseDataInInputTextArea(cz.mp.zxs.tools.data2tap.gui.MainFrame.Radix) 
     * @see #fillDataToInputTextArea(byte[], cz.mp.zxs.tools.data2tap.gui.MainFrame.Radix) 
     */
    private void convertDataInInputTextAreaToNewRadix(Radix origRadix, Radix newRadix) 
            throws InvalidDataException {
        if (origRadix == null) {
            throw new IllegalArgumentException("origRadix");
        }
        if (newRadix == null) {
            throw new IllegalArgumentException("newRadix");
        }
        log.info("origRadix = " + origRadix + "  ->  newRadix = " + newRadix);
        
        byte[] data = parseDataInInputTextArea(origRadix);
        fillDataToInputTextArea(data, newRadix);
    }

    /**
     * 
     * @param data
     * @param radix 
     * @throws IllegalArgumentException
     * @see #parseDataInInputTextArea(cz.mp.zxs.tools.data2tap.gui.MainFrame.Radix) 
     */
    private void fillDataToInputTextArea(byte[] data, Radix radix) {
        if (data == null) {
            throw new IllegalArgumentException("data");
        }
        if (data.length == 0) {
            dataTextArea.setText("");
            return;
        }
        if (radix == null) {
            throw new IllegalArgumentException("radix");
        }
        log.info("radix = " + radix);
        log.info("data.length = " + data.length);
        
        StringBuilder sb = new StringBuilder(data.length * 5);
        
        if (radix == Radix.HEXADECIMAL) {
            for(int i=0; i<data.length; i++) {
                sb.append(String.format("%02X ", (int)(data[i] & 0xFF)));
                // jen dodatečné formátování pro lepší přehlednost
                if ((i+1) % 8 == 0) {
                    sb.append(" ");
                    if ((i+1) % 16 == 0) {
                        sb.append("\n");
                        if ((i+1) % 256 == 0) {
                            sb.append("\n");
                        }
                    }
                }                
            }
        }
        else if (radix == Radix.DECIMAL) {
            for(int i=0; i<data.length; i++) {
                sb.append(String.format("%d, ", (int)(data[i] & 0xFF)));
                // jen dodatečné formátování pro lepší přehlednost
                if ((i+1) % 8 == 0) {
                    sb.append("\n");
                    if ((i+1) % 80 == 0) {
                        sb.append("\n");
                    }
                }
            }            
        }
        else {
            throw new IllegalArgumentException("unsupported radix");
        }
        
        dataTextArea.setText(sb.toString());
    }
    
    /**
     * Z čísel v textové podobě v {@linkplain #dataTextArea} vytvoří 
     * pole bytů.
     * 
     * @param radix
     * @return  pole bytů nebo prázdné pole
     * @throws InvalidDataException 
     * @throws IllegalArgumentException
     * @see #splitDataInInputTextField()
     * @see #parseData(java.lang.String[], cz.mp.zxs.tools.data2tap.gui.MainFrame.Radix) 
     */
    private byte[] parseDataInInputTextArea(Radix radix)    // pozn. 'radix' je potřeba v convertDataInInputTextAreaToNewRadix ...
            throws InvalidDataException {
        if (radix == null) {
            throw new IllegalArgumentException("radix");
        }
        log.debug("radix = " + radix);
        
        String[] dataArray = splitDataInInputTextField();
        if (dataArray.length == 0) {
            return new byte[]{};
        }
        log.debug("dataArray.length = " + dataArray.length);

        byte[] result = parseData(dataArray, radix);

        return result;
    }
    
    /**
     * Analyzuje pole dat v textové podobě a převede je na pole bytů.
     * 
     * @param dataArray
     * @param radix
     * @return 
     */
    private static byte[] parseData(String[] dataArray, Radix radix) 
            throws InvalidDataException {
        if (dataArray.length == 0) {
            return new byte[]{};
        }
        log.debug("dataArray.length = " + dataArray.length);
        
        byte[] result = new byte[dataArray.length];
        
        for (int i=0; i<dataArray.length; i++) {
            try {
                int value = Integer.parseUnsignedInt(dataArray[i], radix.getRadix());
                if (value > 0xFF) {
                    log.warn("value = " + value);
                    throw new InvalidDataException("value " + value + " > 0xFF");
                }
                result[i] = (byte) value;
            } catch (NumberFormatException ex) {
                log.warn(ex.getMessage(), ex);
                throw new InvalidDataException("Wrong number: \"" + dataArray[i] + "\"");
            }
        }
        
        return result;
    }
            
    /**
     * Načte binární soubor obsah zapíše v číselné podobě do 
     * textového pole {@linkplain #dataTextArea}.
     * 
     * @see #loadDataBtn
     */
    private void loadBinaryFileWithDialogs() {
        log.debug("");
        if (loadBinaryFileChooser == null) {
            loadBinaryFileChooser = FileDialogBuilder.createLoadBinaryFileChooser();
        }

        if (loadBinaryFileChooser.showOpenDialog(MainFrame.this.frame) ==
                JFileChooser.APPROVE_OPTION) {                    

            File selectedFile = loadBinaryFileChooser.getSelectedFile();
            if (selectedFile == null || 
                    !selectedFile.exists() || 
                    selectedFile.isDirectory()) {
                log.info("illegal file selected");
                return;
            }

            log.info("selectedFile = " + selectedFile.getName());

            try {
                loadBinaryFileAndFillDataArea(selectedFile);
            } catch (IOException ex) {
                log.warn(ex.getMessage(), ex);
                JOptionPane.showMessageDialog(MainFrame.this.frame,
                        ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        else {
            log.info("canceled");
        }
    }
            
    /**
     * 
     * @param file
     * @throws FileNotFoundException
     * @throws IOException 
     * @throws IllegalArgumentException
     * @see #loadBinaryFileWithDialogs()
     */
    private void loadBinaryFileAndFillDataArea(File file) 
            throws FileNotFoundException, IOException {
        if (file == null) {
            throw new IllegalArgumentException("file=null");
        }
        log.info("file = " + file.getAbsolutePath());
        
        int fileSize = (int) Files.size(file.toPath());
        log.info("fileSize = " + fileSize);
        
        if (fileSize > ZxsConstants.RAM_SIZE__48K) {
            log.warn(file.getName() 
                    + " is too big (" + fileSize + " B > " 
                    + ZxsConstants.RAM_SIZE__48K + " B)");
            throw new IOException("File " + file.getName() 
                    + " is too big (" + fileSize + " B > " 
                    + ZxsConstants.RAM_SIZE__48K + " B)");
        }
            
        Radix selectedRadix = (Radix) dataRadixCombo.getSelectedItem();

        byte[] data = Files.readAllBytes(file.toPath());
        
        fillDataToInputTextArea(data, selectedRadix);
     
        log.info(file.getName() + " loaded");
        JOptionPane.showMessageDialog(frame,
                "<HTML><B>" + file.getName() + "</B> loaded.<BR>"
                + "Size = " + data.length + " B", 
                "",
                JOptionPane.INFORMATION_MESSAGE);
    }
            
    /**
     * Kontroluje adresu zadanou v {@linkplain #adressCombo}.
     *
     * @return  {@code false}, pokud je zadaná adresa špatně, jinak {@code true}
     * @see #validateAllInputValuesAndShowDialogs() 
     * @see #createTapFileWithDialogs()
     */
    private boolean validateAdressAndShowDialogs() {
        int adress = 0;
        try {
            adress = getSelectedAdress();
            if (adress < 0x4000 || adress > 0xFFFF) {
                log.warn("Illegal adress " + adress);
                JOptionPane.showMessageDialog(MainFrame.this.frame,
                        "Adress have to be from 0x4000 to 0xFFFF (from 16384 to 65535)", 
                        "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } catch (NumberFormatException nex) {
            log.warn("Wrong adress number. " + nex.getMessage(), nex);
            JOptionPane.showMessageDialog(MainFrame.this.frame,
                    "Wrong adress number (" + nex.getMessage() + ")",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        return true;
    }
    
    
    /**
     * 
     * @return  vybraný tap soubor
     * @see createTapAndShowDialogs()
     */
    private File selectTapFileWithDialog() {
        File tapFile = null;
        if (saveTapFileChooser == null) {
            saveTapFileChooser = FileDialogBuilder.createSaveTapFileChooser();
        }

        if (saveTapFileChooser.showSaveDialog(MainFrame.this.frame) ==
                JFileChooser.APPROVE_OPTION) {                    
            
            tapFile = saveTapFileChooser.getSelectedFile();
            log.info("out tapFile = " + tapFile.getAbsolutePath());            
            //FileFilter selFFilter = saveTapFileChooser.getFileFilter();
            //log.info("" + selFFilter.toString() + " " + selFFilter.getDescription());
            //if (selFFilter instanceof ExtFileFilter) {
            //    ExtFileFilter selExtFF = (ExtFileFilter) selFFilter;
            //    if (selExtFF.getExtensionsList()[0].equalsIgnoreCase("tap")) {
            //        // vybrát filt tap souborů
            //    }
            //}
            // přidat příponu tap, pokud ji soubor nemá
            String ext = FileUtils.getFileExtension(tapFile);
            if (! ext.toLowerCase().equals("tap")) {
                tapFile = new File(tapFile.getAbsolutePath() + ".tap");
            }
            log.info("out tapFile = " + tapFile.getAbsolutePath());
            
            // soubor již existuje, přepsat?
            if (tapFile.exists() && !tapFile.isDirectory()) {
                log.info(tapFile.getName() + " exists. Overwrite?");
                int res = JOptionPane.showConfirmDialog(MainFrame.this.frame,
                        "<HTML><B>" + tapFile.getName() + "</B> already exists. Overwrite?", 
                        "", JOptionPane.YES_NO_OPTION);
                if (res != JOptionPane.YES_OPTION) {
                    log.info("canceled");
                    return null;
                }
            }           
        }
        else {
            log.info("canceled");
            return null;
        }
        log.debug("File selected");
        return tapFile;
    }
    
    /**
     * 
     * @param outTapFile
     * @param tapHeader
     * @param tapBody
     * @return 
     * @see createTapAndShowDialogs()
     */
    private boolean saveOutputTapAndShowDialogs(File outTapFile, 
            TapHeader tapHeader, TapBody tapBody) {
        try (
            FileOutputStream fos = new FileOutputStream(outTapFile);
            ) {

            fos.write(tapHeader.getBytes());
            fos.write(tapBody.getBytes());

            fos.flush();

            JOptionPane.showMessageDialog(frame,
                    "<HTML><B>" + outTapFile.getName() + "</B> successfully created.", 
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            return true;            
        } catch (IOException ioex) {
            log.warn(ioex.getMessage(), ioex);
            JOptionPane.showMessageDialog(MainFrame.this.frame,
                    ioex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);            
            return false;            
        }               
    }
    
    /**
     * Ze všech zadaných dat Sestaví TAP soubor.
     * Zobrazí všechny potřebné dialogy.
     */
    private void createTapFileWithDialogs() {
        log.info("");

        // --- 1. validace vstupních hodnot
        String name = nameField.getText();
        if (name.length() > 10) {   // kvůli FormattedTextField by nemělo nikdy nastat 
            log.warn("name.length() > 10");
            JOptionPane.showMessageDialog(MainFrame.this.frame,
                    "Name is too long", "Error", JOptionPane.ERROR_MESSAGE);            
            return;
        }
        
        if (validateAdressAndShowDialogs() == false) {
            return;
        }        
        int adress = getSelectedAdress();
        
        Radix selectedRadix = (Radix) dataRadixCombo.getSelectedItem();
        
        byte[] inputData;
        try {
            inputData = parseDataInInputTextArea(selectedRadix);
        }
        catch (InvalidDataException idex) {
            log.warn(idex.getMessage(), idex);
            JOptionPane.showMessageDialog(MainFrame.this.frame,
                    idex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);            
            return;
        }

        if (adress + inputData.length > ADR_MAX__48K) {
            log.warn("adress + inputData.length > ADR_MAX__48K");
            JOptionPane.showMessageDialog(MainFrame.this.frame,
                    "Data overflows RAM size (Adress + \"length of data\" > " + ADR_MAX__48K + ")",
                    "Error", JOptionPane.ERROR_MESSAGE);                                  
            return;
        }
        log.debug("validation finished");
        
        // --- 2. dialog pro zadání výstupního souboru
        File outTapFile = selectTapFileWithDialog();
        if (outTapFile == null) {
            return;
        }

        // --- 3. připravení obsahu TAP
        TapHeader tapHeader = new TapHeader();
        tapHeader.setType((TapBlockType)typeCombo.getSelectedItem());
        tapHeader.setName(name);
        tapHeader.setDataLength(inputData.length);
        tapHeader.setParam1(adress);
        tapHeader.createData();

        TapBody tapBody = new TapBody(inputData.length);
        tapBody.append(inputData);
        tapBody.appendParityToLastByte();

        log.debug("tap prepared");
        
        // --- 4. uložení do souboru
        boolean ok = saveOutputTapAndShowDialogs(outTapFile, tapHeader, tapBody);
        if (!ok) {
            return;
        }
        log.info(outTapFile.getName() + " successfully created");
    }

    /**
     * 
     * @return
     * @throws NumberFormatException 
     * @see #adressCombo
     */
    private int getSelectedAdress() throws NumberFormatException {
        String rawAdress = adressCombo.getSelectedItem().toString().trim();
        log.debug("rawAdress = " + rawAdress);
        int adress;
        if (rawAdress.startsWith("0x")) {
            adress = Integer.parseInt(rawAdress.substring(2), 16);
        }
        else {
            adress = Integer.parseInt(rawAdress, 10);
        }
        log.info("adress = " + adress);
        return adress;
    }

    /**
     * Rozdělí data v textové podobě v {@linkplain #dataTextArea} na 
     * jednotlivé prvky.
     * <p>
     * např. {@literal " 24 60,90" -> ["24","60","90"]}
     * 
     * @return 
     */
    private String[] splitDataInInputTextField() {
        String rawData = dataTextArea.getText().trim();
        return splitDataText(rawData);
    }
    
    /**
     * Rozdělí data v textové podobě na jednotlivé prvky.
     * <p>
     * např. {@literal " 24 60,90" -> ["24","60","90"]}
     * 
     * @param data
     * @return 
     */
    private static String[] splitDataText(String data) {
        if (data.isEmpty()) {
            return new String[]{};
        }
        String[] dataArray = data.split("[,.;\\s\\r\\n]+");
        //log.debug("dataArray = " + Arrays.toString(dataArray));
        log.info("dataArray.length = " + dataArray.length);
        return dataArray;
    }

    // -------------------------------------------------------------------------
    
    /**
     * 
     * @see #adressCombo
     * @see ValueDescriptionCellRenderer
     */
    private class ValueDescriptionItem {
        private String value;
        private String description;
        
        private ValueDescriptionItem (String value, String description) {
            this.value = value;
            this.description = description;
        }

        public String getValue() {
            return value;
        }
        
        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return value;
        }
    }
    
    /**
     * 
     * @see #adressCombo
     * @see ValueDescriptionItem
     */
    private class ValueDescriptionCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(
                JList list, Object value,  
                int index, boolean isSelected, boolean cellHasFocus) {

            Component defaultListCellRenderer = super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);

            if (! (defaultListCellRenderer instanceof JLabel)) {
                return defaultListCellRenderer;
            }
            if (value == null) {
                return defaultListCellRenderer;
            }
            if (! (value instanceof ValueDescriptionItem)) {
                return defaultListCellRenderer;
            }            
            JLabel result = (JLabel) defaultListCellRenderer;

            ValueDescriptionItem valueDesc = (ValueDescriptionItem) value;            
            if (valueDesc.getDescription().isEmpty()) {
                result.setText(valueDesc.getValue() + " ");
            }
            else {
                //result.setText(adrVal.getValue() 
                //        + "  (" + adrVal.getDescription() + ")");                
                result.setText("<HTML>" + valueDesc.getValue() 
                        + "  <FONT color=\"GRAY\">(" + valueDesc.getDescription() + ")</font>");
            }
                    
            return result;        
        }
    }

    /**
     * 
     * @see #dataRadixCombo
     * @see ChangeDataRadixItemListener
     */
    private enum Radix {
        DECIMAL(10, "Decimal"),
        HEXADECIMAL(16, "Hexadecimal"),
        ;
        
        int radix;
        String name;
        Radix(int radix, String name) {
            this.radix = radix;
            this.name = name;
        }

        public int getRadix() {
            return radix;
        }

        public String getName() {
            return name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Obsluha události "Změna výběru číselného základu" v {@linkplain dataRadixCombo}.
     * 
     * @see #dataRadixCombo
     */
    private class ChangeDataRadixItemListener implements ItemListener {
        private Radix origRadix = (Radix) dataRadixCombo.getSelectedItem();

        @Override
        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange()==ItemEvent.SELECTED) {
                final Radix newRadix = (Radix) dataRadixCombo.getSelectedItem();

                log.info("origRadix = " + origRadix + "  ->  newRadix = " + newRadix);

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            MainFrame.this.convertDataInInputTextAreaToNewRadix(
                                    origRadix, newRadix);
                            origRadix = newRadix;
                        } catch (InvalidDataException idex) {
                            log.warn(idex.getMessage(), idex);
                            JOptionPane.showMessageDialog(MainFrame.this.frame,
                                    idex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }   
                });                    
            }
        } 
    }   // ChangeDataRadixItemListener
    
}   // MainFrame.java
