//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import net.sf.gogui.go.ConstPointList;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import static net.sf.gogui.go.GoColor.EMPTY;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.PointList;
import net.sf.gogui.gtp.AnalyzeCommand;
import net.sf.gogui.gtp.AnalyzeDefinition;
import net.sf.gogui.gtp.AnalyzeType;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gtp.GtpResponseFormatError;
import net.sf.gogui.gtp.GtpUtil;
import net.sf.gogui.util.Platform;
import net.sf.gogui.util.PrefUtil;

/** Dialog for selecting an AnalyzeCommand. */
public final class AnalyzeDialog
    extends JDialog
    implements ActionListener, ListSelectionListener
{
    /** Callback for actions generated by AnalyzeDialog. */
    public interface Listener
    {
        void actionClearAnalyzeCommand();

        void actionSetAnalyzeCommand(AnalyzeCommand command, boolean autoRun,
                                     boolean clearBoard, boolean oneRunOnly);
    }

    public AnalyzeDialog(Frame owner, Listener listener,
                         ArrayList<AnalyzeDefinition> commands,
                         GuiGtpClient gtp, MessageDialogs messageDialogs)
    {
        super(owner, "Analyze");
        m_messageDialogs = messageDialogs;
        m_gtp = gtp;
        m_commands = commands;
        m_listener = listener;
        Container contentPane = getContentPane();
        JPanel commandPanel = createCommandPanel();
        contentPane.add(commandPanel, BorderLayout.CENTER);
        comboBoxChanged();
        setSelectedColor(BLACK);
        int minWidth = commandPanel.getPreferredSize().width;
        setMinimumSize(new Dimension(minWidth, 192));
        pack();
        addWindowListener(new WindowAdapter() {
                public void windowActivated(WindowEvent e) {
                    m_comboBoxHistory.requestFocusInWindow();
                }
            });
    }

    public void actionPerformed(ActionEvent event)
    {
        String command = event.getActionCommand();
        if (command.equals("clear"))
            clearCommand();
        else if (command.equals("comboBoxChanged"))
            comboBoxChanged();
        else if (command.equals("run"))
            runCommand();
        else
            assert false;
    }

    public void dispose()
    {
        if (! m_autoRun.isSelected())
            clearCommand();
        saveRecent();
        super.dispose();
    }

    public GoColor getSelectedColor()
    {
        if (m_black.isSelected())
            return BLACK;
        else
            return WHITE;
    }

    public void saveRecent()
    {
        ArrayList<String> recent = new ArrayList<String>(MAX_SAVE_RECENT);
        int start = (m_firstIsTemp ? 1 : 0);
        for (int i = start; i < getComboBoxItemCount(); ++i)
        {
            String name = getComboBoxItem(i);
            if (recent.indexOf(name) < 0)
                recent.add(name);
        }
        for (int i = 0; i < m_fullRecentList.size(); ++i)
        {
            if (recent.size() == MAX_SAVE_RECENT)
                break;
            String name = m_fullRecentList.get(i);
            if (recent.indexOf(name) < 0)
                recent.add(name);
        }
        PrefUtil.putList("net/sf/gogui/gui/analyzedialog/recentcommands",
                         recent);
    }

    /** Set board size.
        Need for verifying responses to initial value for EPLIST commands.
        Default is 19.
    */
    public void setBoardSize(int boardSize)
    {
        m_boardSize = boardSize;
    }

    public void setSelectedColor(GoColor color)
    {
        m_selectedColor = color;
        selectColor();
    }

    public void valueChanged(ListSelectionEvent e)
    {
        int index = m_list.getSelectedIndex();
        if (index >= 0)
            selectCommand(index);
    }

    private static final int MAX_SAVE_RECENT = 100;

    /** Is the first item in the history combo box a temporary item?
        Avoids that the first item in the history combo box is treated
        as a real history command, if it was not run.
    */
    private boolean m_firstIsTemp;

    private int m_boardSize = GoPoint.DEFAULT_SIZE;

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sf.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private ArrayList<String> m_fullRecentList;

    private GoColor m_selectedColor = EMPTY;

    private final MessageDialogs m_messageDialogs;

    private final GuiGtpClient m_gtp;

    private JButton m_clearButton;

    private JButton m_runButton;

    private JCheckBox m_autoRun;

    private JCheckBox m_clearBoard;

    private JComboBox m_comboBoxHistory;

    private JList m_list;

    private Box m_colorBox;

    private JRadioButton m_black;

    private JRadioButton m_white;

    private final ArrayList<AnalyzeDefinition> m_commands;

    private final Listener m_listener;

    private String m_lastUpdateOptionsCommand;

    private void clearCommand()
    {
        m_listener.actionClearAnalyzeCommand();
        m_autoRun.setSelected(false);
    }

    private void comboBoxChanged()
    {
        Object item = m_comboBoxHistory.getSelectedItem();
        if (item == null)
        {
            m_list.clearSelection();
            return;
        }
        String label = item.toString();
        updateOptions(label);
        String selectedValue = (String)m_list.getSelectedValue();
        if (selectedValue != null && ! selectedValue.equals(label))
            m_list.clearSelection();
    }

    private JPanel createButtons()
    {
        JPanel innerPanel = new JPanel(new GridLayout(1, 0, GuiUtil.PAD, 0));
        m_runButton = new JButton("Run");
        m_runButton.setToolTipText("Run command");
        m_runButton.setActionCommand("run");
        m_runButton.addActionListener(this);
        m_runButton.setMnemonic(KeyEvent.VK_R);
        m_runButton.setEnabled(false);
        GuiUtil.setMacBevelButton(m_runButton);
        innerPanel.add(m_runButton);
        m_clearButton = new JButton("Clear");
        m_clearButton.setToolTipText("Clear board and cancel auto run");
        m_clearButton.setActionCommand("clear");
        m_clearButton.addActionListener(this);
        m_clearButton.setMnemonic(KeyEvent.VK_C);
        GuiUtil.setMacBevelButton(m_clearButton);
        innerPanel.add(m_clearButton);
        JPanel outerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        outerPanel.add(innerPanel);
        return outerPanel;
    }

    private JComponent createColorPanel()
    {
        m_colorBox = Box.createVerticalBox();
        ButtonGroup group = new ButtonGroup();
        m_black = new JRadioButton("Black");
        m_black.setToolTipText("Run selected command for color Black");
        m_black.setEnabled(false);
        group.add(m_black);
        m_colorBox.add(m_black);
        m_white = new JRadioButton("White");
        m_white.setToolTipText("Run selected command for color White");
        m_white.setEnabled(false);
        group.add(m_white);
        m_colorBox.add(m_white);
        return m_colorBox;
    }

    private JPanel createCommandPanel()
    {
        JPanel panel = new JPanel(new BorderLayout());
        m_list = new JList();
        m_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_list.setVisibleRowCount(25);
        m_list.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    int modifiers = e.getModifiers();
                    int mask = ActionEvent.ALT_MASK;
                    if (e.getClickCount() == 2
                        || ((modifiers & mask) != 0))
                    {
                        //int index =
                        //   m_list.locationToIndex(event.getPoint());
                        runCommand();
                    }
                }
            });
        m_list.addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) {
                    int index = getSelectedCommand();
                    if (index >= 0)
                        m_list.setSelectedIndex(index);
                }
            });
        m_list.addListSelectionListener(this);
        JScrollPane scrollPane = new JScrollPane(m_list);
        if (Platform.isMac())
            // Default Apple L&F uses no border, but Quaqua 3.7.4 does
            scrollPane.setBorder(null);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(createLowerPanel(), BorderLayout.SOUTH);
        String[] labels = new String[m_commands.size()];
        for (int i = 0; i < m_commands.size(); ++i)
            labels[i] = m_commands.get(i).getLabel();
        m_list.setListData(labels);
        comboBoxChanged();
        loadRecent();
        return panel;
    }

    private JPanel createLowerPanel()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(GuiUtil.createFiller());
        m_comboBoxHistory = new JComboBox();
        panel.add(m_comboBoxHistory);
        JPanel lowerPanel = new JPanel();
        lowerPanel.setLayout(new BoxLayout(lowerPanel, BoxLayout.Y_AXIS));
        lowerPanel.setBorder(GuiUtil.createEmptyBorder());
        panel.add(lowerPanel);
        JPanel optionsPanel
            = new JPanel(new GridLayout(0, 2, GuiUtil.PAD, 0));
        lowerPanel.add(optionsPanel);
        JPanel leftPanel = new JPanel();
        optionsPanel.add(leftPanel);
        Box leftBox = Box.createVerticalBox();
        leftPanel.add(leftBox);
        m_autoRun = new JCheckBox("Auto run");
        m_autoRun.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (! m_autoRun.isSelected())
                        m_listener.actionClearAnalyzeCommand();
                }
            });
        m_autoRun.setToolTipText("Automatically run after changes on board");
        m_autoRun.setEnabled(false);
        leftBox.add(m_autoRun);
        m_clearBoard = new JCheckBox("Clear board");
        m_clearBoard.setToolTipText("Clear board before displaying result");
        m_clearBoard.setEnabled(false);
        leftBox.add(m_clearBoard);
        m_clearBoard.setSelected(true);
        JPanel rightPanel = new JPanel();
        rightPanel.add(createColorPanel());
        optionsPanel.add(rightPanel);
        lowerPanel.add(createButtons());
        m_comboBoxHistory.addActionListener(this);
        return panel;
    }

    private String getComboBoxItem(int i)
    {
        return m_comboBoxHistory.getItemAt(i).toString();
    }

    private int getComboBoxItemCount()
    {
        return m_comboBoxHistory.getItemCount();
    }

    private int getCommandIndex(String label)
    {
        for (int i = 0; i < m_commands.size(); ++i)
            if (m_commands.get(i).getLabel().equals(label))
                return i;
        return -1;
    }

    private int getSelectedCommand()
    {
        Object item = m_comboBoxHistory.getSelectedItem();
        if (item == null)
            return -1;
        return getCommandIndex(item.toString());
    }

    private void insertComboBoxItem(String label, int index)
    {
        m_comboBoxHistory.insertItemAt(GuiUtil.createComboBoxItem(label),
                                       index);
    }

    private void loadRecent()
    {
        m_comboBoxHistory.removeAllItems();
        m_fullRecentList =
            PrefUtil.getList("net/sf/gogui/gui/analyzedialog/recentcommands");
        for (int i = 0; i < m_fullRecentList.size(); ++i)
        {
            String name = m_fullRecentList.get(i);
            if (getCommandIndex(name) >= 0)
                m_comboBoxHistory.addItem(GuiUtil.createComboBoxItem(name));
            if (m_comboBoxHistory.getItemCount() > 20)
                break;
        }
        int index = getSelectedCommand();
        if (index >= 0)
            selectCommand(index);
        m_firstIsTemp = false;
    }

    private void runCommand()
    {
        if (m_gtp.isCommandInProgress())
        {
            showError("Cannot execute while computer is thinking",
                      "You need to wait until the command in "
                      + " progress is finished.",
                      false);
            return;
        }
        int index = getSelectedCommand();
        if (index < 0)
        {
            String name = m_gtp.getName();
            if (name == null)
                name = "The current Go program";
            showError("Command not supported",
                      name + " does not support this command.", false);
            return;
        }
        updateRecent(index);
        AnalyzeCommand command = new AnalyzeCommand(m_commands.get(index));
        if (command.needsColorArg())
            command.setColorArg(getSelectedColor());
        String label = command.getResultTitle();
        if (command.needsStringArg())
        {
            String stringArg =
                JOptionPane.showInputDialog(this, label, "Input",
                                            JOptionPane.PLAIN_MESSAGE);
            if (stringArg == null)
                return;
            command.setStringArg(stringArg);
        }
        if (command.needsOptStringArg())
        {
            command.setOptStringArg("");
            String commandWithoutArg =
                command.replaceWildCards(m_selectedColor);
            try
            {
                String value = m_gtp.send(commandWithoutArg);
                Object optStringArg =
                    JOptionPane.showInputDialog(this, label, "Input",
                                                JOptionPane.PLAIN_MESSAGE,
                                                null, null, value);
                if (optStringArg == null || optStringArg.equals(value))
                    return;
                command.setOptStringArg((String)optStringArg);
            }
            catch (GtpError e)
            {
                showError("Command \"" + commandWithoutArg + "\" failed",
                          e.getMessage(), false);
                return;
            }
        }
        if (command.getType() == AnalyzeType.EPLIST)
        {
            command.setPointListArg(new PointList());
            String commandWithoutArg =
                command.replaceWildCards(m_selectedColor) + " show";
            try
            {
                String response = m_gtp.send(commandWithoutArg);
                ConstPointList pointList =
                    GtpUtil.parsePointList(response, m_boardSize);
                command.setPointListArg(pointList);
            }
            catch (GtpError e)
            {
                showError("Command \"" + commandWithoutArg + "\" failed",
                          e.getMessage(), true);
                return;
            }
            catch (GtpResponseFormatError e)
            {
                showError("Invalid response to command \""
                          + commandWithoutArg + "\"",
                          "The response had an unexpected format ("
                          + e.getMessage() + ").", true);
                return;
            }
        }
        if (command.needsFileArg())
        {
            File fileArg = FileDialogs.showSelectFile(this, label);
            if (fileArg == null)
                return;
            command.setFileArg(fileArg);
        }
        if (command.needsFileOpenArg())
        {
            File fileArg = FileDialogs.showOpen(this, label);
            if (fileArg == null)
                return;
            command.setFileOpenArg(fileArg);
        }
        if (command.needsFileSaveArg())
        {
            File fileArg = FileDialogs.showSave(this, label, m_messageDialogs);
            if (fileArg == null)
                return;
            command.setFileSaveArg(fileArg);
        }
        if (command.needsColorArg())
            command.setColorArg(getSelectedColor());
        boolean autoRun = m_autoRun.isEnabled() && m_autoRun.isSelected();
        boolean clearBoard =
            ! m_clearBoard.isEnabled() || m_clearBoard.isSelected();
        m_listener.actionSetAnalyzeCommand(command, autoRun, clearBoard,
                                           false);
    }

    private void selectCommand(int index)
    {
        String label = m_commands.get(index).getLabel();
        updateOptions(label);
        m_comboBoxHistory.removeActionListener(this);
        if (m_firstIsTemp && getComboBoxItemCount() > 0)
            m_comboBoxHistory.removeItemAt(0);
        if (getComboBoxItemCount() == 0 || ! getComboBoxItem(0).equals(label))
        {
            insertComboBoxItem(label, 0);
            m_firstIsTemp = true;
            m_comboBoxHistory.setSelectedIndex(0);
        }
        m_comboBoxHistory.addActionListener(this);
    }

    private void selectColor()
    {
        if (m_selectedColor == BLACK)
            m_black.setSelected(true);
        else if (m_selectedColor == WHITE)
            m_white.setSelected(true);
    }

    private void showError(String mainMessage, String optionalMessage)
    {
        showError(mainMessage, optionalMessage, true);
    }

    private void showError(String mainMessage, String optionalMessage,
                           boolean isCritical)
    {
        m_messageDialogs.showError(this, mainMessage, optionalMessage,
                                   isCritical);
    }

    private void updateOptions(String label)
    {
        if (label.equals(m_lastUpdateOptionsCommand))
            return;
        m_lastUpdateOptionsCommand = label;
        int index = getCommandIndex(label);
        if (index < 0)
            return;
        AnalyzeCommand command =
            new AnalyzeCommand(m_commands.get(index));
        boolean needsColorArg = command.needsColorArg();
        m_black.setEnabled(needsColorArg);
        m_white.setEnabled(needsColorArg);
        m_autoRun.setEnabled(command.getType() != AnalyzeType.PARAM);
        m_autoRun.setSelected(false);
        m_clearBoard.setEnabled(command.getType() != AnalyzeType.PARAM);
        m_runButton.setEnabled(true);
    }

    private void updateRecent(int index)
    {
        String label = m_commands.get(index).getLabel();
        insertComboBoxItem(label, 0);
        for (int i = 1; i < getComboBoxItemCount(); ++i)
            if (getComboBoxItem(i).equals(label))
                m_comboBoxHistory.removeItemAt(i);
        m_comboBoxHistory.setSelectedIndex(0);
        m_firstIsTemp = false;
    }
}
