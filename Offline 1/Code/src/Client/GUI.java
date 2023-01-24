package Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class GUI extends JFrame {
    // gui components
    JPanel panel_up;
    JPanel panel_down;
    JButton upload_btn;

    // procedure components
    ClientIO cio;
    Integer ID;
    File storage;

    public GUI(ClientIO cio){
        this.cio = cio;
//        this.ID = ID;
        panel_up = login_page();

        panel_down = new JPanel();
        panel_down.setBackground(new Color(27, 193, 118));
        panel_down.setBounds(0, 220, 720, 500);
        panel_down.setLayout(new BorderLayout());

        this.setTitle("LOGIN");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(null);
        this.add(panel_up);
        this.add(panel_down);
        this.setSize(720, 720);
        this.toFront();

        // console print
        JTextArea console_print = new JTextArea();
        console_print.setBackground(new Color(27, 193, 118));
        console_print.setFont(new Font("Consolas", Font.BOLD, 17));
        redirectSystemStreams(console_print);
        JScrollPane console = new JScrollPane(console_print, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        console.setSize(710, 465);
        panel_down.add(console);

        this.setVisible(true);
    }

    private JPanel login_page(){
        JPanel panel = new JPanel();
        panel.setBackground(new Color(125, 221, 233));
        panel.setBounds(0, 0, 720, 220);
        panel.setLayout(new GridLayout(2, 1));

        // row 1
        JPanel flow1 = new JPanel();
        flow1.setBackground(new Color(125, 221, 233));
        flow1.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 15));

        JLabel lbl1 = new JLabel("Student ID :");
        lbl1.setFont(new Font("Consolas", Font.BOLD, 20));
        flow1.add(lbl1);
        JTextField sID = new JTextField();
        sID.setPreferredSize(new Dimension(100, 30));
        sID.setFont(new Font("Consolas", Font.BOLD, 20));
        flow1.add(sID);

        // row 2
        JPanel flow2 = new JPanel();
        flow2.setBackground(new Color(125, 221, 233));
        flow2.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton LOGIN_BTN = getButton(0, 0, 80, 30, "LOG IN");
        LOGIN_BTN.addActionListener(e->{
            if(cio.login(sID.getText())){
                this.ID = Integer.valueOf(sID.getText());
                this.setTitle("Welcome Student ID: " + this.ID);
                this.storage = new File(Client.LOCAL_STORAGE.getAbsolutePath() + '/' + this.ID);
                this.storage.mkdir();
                this.cio.fsys.setDownload_path(this.storage.getAbsolutePath());
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        replacePanel(home_page());
                    }
                });
            }else{
                try{
                    System.err.println(">>WARNING: You logged in from a different device.");
                    this.dispose();
                }catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });
        flow2.add(LOGIN_BTN);

        panel.add(flow1);
        panel.add(flow2);
        return panel;
    }

    private JPanel home_page(){
        JPanel panel = new JPanel();
        panel.setBackground(new Color(125, 221, 233));
        panel.setBounds(0, 0, 720, 220);
        panel.setLayout(new GridLayout(4, 1));

        // row 1
        JPanel flow1 = new JPanel();
        flow1.setBackground(new Color(125, 221, 233));
        flow1.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton UPLOAD_BTN = getButton(30, 30, 100, 50, "UPLOAD");
        UPLOAD_BTN.addActionListener(e->{
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    replacePanel(upload_page());
                }
            } );
        });
        flow1.add(UPLOAD_BTN);

        JButton LOOKUP1_BTN = getButton(30, 30, 100, 50, "Student List");
        LOOKUP1_BTN.addActionListener(e->{
            cio.setTask("STUDENT_LIST");
            new Thread(cio).start();
        });
        flow1.add(LOOKUP1_BTN);

        // row 2
        JPanel flow2 = new JPanel();
        flow2.setBackground(new Color(125, 221, 233));
        flow2.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton LOOKUP2_BTN = getButton(30, 30, 100, 50, "My Files");
        LOOKUP2_BTN.addActionListener(e->{
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    replacePanel(download_page(false));
                }
            });
        });
        flow2.add(LOOKUP2_BTN);

        JButton LOOKUP3_BTN = getButton(30, 30, 100, 50, "Others Files");
        LOOKUP3_BTN.addActionListener(e->{
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    replacePanel(download_page(true));
                }
            });
        });
        flow2.add(LOOKUP3_BTN);

        // row 3
        JPanel flow3 = new JPanel();
        flow3.setBackground(new Color(125, 221, 233));
        flow3.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton REQ_BTN = getButton(30, 30, 100, 50, "Request File");
        REQ_BTN.addActionListener(e->{
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    replacePanel(request_page());
                }
            });
        });
        flow3.add(REQ_BTN);

        JButton MSG_BTN = getButton(30, 30, 100, 50, "Messages");
        MSG_BTN.addActionListener(e->{
            this.cio.setTask("READ");
            new Thread(cio).start();
        });
        flow3.add(MSG_BTN);

        // row 3
        JPanel flow4 = new JPanel();
        flow4.setBackground(new Color(125, 221, 233));
        flow4.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton LOGOUT_BTN = getButton(30, 30, 100, 50, "Logout");
        LOGOUT_BTN.addActionListener(e->{
            this.cio.setTask("LOGOUT");
            System.err.println(">>Logging out");
            cio.run();
            this.dispose();
        });
        flow4.add(LOGOUT_BTN);

        panel.add(flow1);
        panel.add(flow2);
        panel.add(flow3);
        panel.add(flow4);
        return panel;
    }

    private JPanel upload_page(){
        JPanel panel = new JPanel();
        panel.setBackground(new Color(125, 221, 233));
        panel.setBounds(0, 0, 720, 220);
        panel.setLayout(new GridLayout(3, 1));

        // upper row
        JPanel flow1 = new JPanel();
        flow1.setBackground(new Color(125, 221, 233));
        flow1.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JLabel lbl1 = new JLabel("Privacy :");
        flow1.add(lbl1);
        String[] choices = { "Public", "Private"};
        final JComboBox<String> cb = new JComboBox<String>(choices);
        flow1.add(cb);

        JLabel lbl2 = new JLabel("RequestID :");
        flow1.add(lbl2);
        JTextField reqID = new JTextField();
        reqID.setPreferredSize(new Dimension(60, 25));
        flow1.add(reqID);
        JLabel lbl_req = new JLabel("(Fill if requested file)");
        flow1.add(lbl_req);

        // lower row
        JPanel flow2 = new JPanel();
        flow2.setBackground(new Color(125, 221, 233));
        flow2.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JTextField filename = new JTextField();
        filename.setPreferredSize(new Dimension(200, 25));
        filename.setEnabled(false);
        flow2.add(filename);

        JButton fbtn = getButton(0, 0, 80, 30, "Choose File");
        JButton up_btn = getButton(0, 0, 80, 30, "UPLOAD");
        up_btn.setEnabled(false);
        fbtn.addActionListener(e->{
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
            if(fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION){
                File selectedFile = fileChooser.getSelectedFile();
                filename.setText(selectedFile.getAbsolutePath());
                up_btn.setEnabled(true);
            }
        });
        flow2.add(fbtn);
        up_btn.addActionListener(e->{
            int getID;
            try{
                if(reqID.getText().equals("")) getID = -1;
                else getID = Integer.parseInt(reqID.getText());
            }catch(NumberFormatException nfe){
                getID = -1;
            }
//            reqID.setText(String.valueOf(getID));
            if(getID != -1) cb.setSelectedIndex(0); // if this is for a request, then always public file
            try{
                this.cio.upload_file(
                        (String)cb.getSelectedItem(),
                        getID,
                        filename.getText()
                );
            }catch(Exception exp){
                exp.printStackTrace();
                System.out.println("Something went wrong while uploading");
            }
        });
        flow2.add(up_btn, BorderLayout.CENTER);

        // row 3
        JPanel flow3 = new JPanel();
        flow3.setBackground(new Color(125, 221, 233));
        flow3.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton back_btn = getButton(0, 0, 80, 30, "BACK");
        back_btn.addActionListener(e->{
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    replacePanel(home_page());
                }
            });
        });
        flow3.add(back_btn, BorderLayout.CENTER);

        panel.add(flow1);
        panel.add(flow2);
        panel.add(flow3);
        return panel;
    }

    private JPanel download_page(boolean other_files){
        JPanel panel = new JPanel();
        panel.setBackground(new Color(125, 221, 233));
        panel.setBounds(0, 0, 720, 220);
        panel.setLayout(new GridLayout(3, 1));

        // row 1
        JPanel flow1 = new JPanel();
        flow1.setBackground(new Color(125, 221, 233));
        flow1.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JLabel lbl1 = new JLabel("Student ID :");
        flow1.add(lbl1);
        JTextField sID = new JTextField();
        sID.setPreferredSize(new Dimension(60, 25));
        if(!other_files){
            sID.setText(String.valueOf(this.ID));
            sID.setEnabled(false);
        }
        flow1.add(sID);

        JButton search_btn = getButton(0, 0, 80, 30, "Search");
        JButton down_btn = getButton(0, 0, 80, 30, "DOWNLOAD");
        down_btn.setEnabled(false);
        search_btn.addActionListener(e->{
            if(sID.getText().equals("")) System.out.println("Please enter Student ID");
            else{
                if(other_files) this.cio.setTask("OTHER_FILE_LIST#" + sID.getText());
                else this.cio.setTask("MY_FILE_LIST");
                new Thread(cio).start();
                down_btn.setEnabled(true);
            }
        });
        flow1.add(search_btn);

        // row 2
        JPanel flow2 = new JPanel();
        flow2.setBackground(new Color(125, 221, 233));
        flow2.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JLabel lbl2 = new JLabel("File ID :");
        flow2.add(lbl2);
        JTextField fileID = new JTextField();
        fileID.setPreferredSize(new Dimension(60, 25));
        flow2.add(fileID);

        down_btn.addActionListener(e->{
            if(fileID.getText().equals("")) System.out.println("Enter valid fileID");
            else{
                cio.setTask("DOWNLOAD#" + fileID.getText() + "#" + sID.getText());
                new Thread(cio).start();
            }
        });
        flow2.add(down_btn, BorderLayout.CENTER);

        // row 3
        JPanel flow3 = new JPanel();
        flow3.setBackground(new Color(125, 221, 233));
        flow3.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton back_btn = getButton(0, 0, 80, 30, "BACK");
        back_btn.addActionListener(e->{
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    replacePanel(home_page());
                }
            });
        });
        flow3.add(back_btn, BorderLayout.CENTER);
        panel.add(flow1);
        panel.add(flow2);
        panel.add(flow3);
        return panel;
    }

    private JPanel request_page(){
        JPanel panel = new JPanel();
        panel.setBackground(new Color(125, 221, 233));
        panel.setBounds(0, 0, 720, 220);
        panel.setLayout(new GridLayout(2, 1));

        // row 1
        JPanel flow1 = new JPanel();
        flow1.setBackground(new Color(125, 221, 233));
        flow1.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JLabel lbl1 = new JLabel("File Description :");
        flow1.add(lbl1);
        JTextArea fileDesc = new JTextArea();
        fileDesc.setPreferredSize(new Dimension(300, 100));
        flow1.add(fileDesc);

        JButton search_btn = getButton(0, 0, 80, 30, "Request");
        search_btn.addActionListener(e->{
            if(fileDesc.getText().equals("")) System.out.println("Please enter file Description");
            else{
                cio.setTask("REQUEST#"+fileDesc.getText());
                new Thread(cio).start();
            }
        });
        flow1.add(search_btn);

        // row 2
        JPanel flow2 = new JPanel();
        flow2.setBackground(new Color(125, 221, 233));
        flow2.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton back_btn = getButton(0, 0, 80, 30, "BACK");
        back_btn.addActionListener(e->{
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    replacePanel(home_page());
                }
            });
        });
        flow2.add(back_btn, BorderLayout.CENTER);
        panel.add(flow1);
        panel.add(flow2);
        return panel;
    }

    private JButton getButton(int x, int y, int w, int h, String text){
        JButton new_btn = new JButton();
        new_btn.setBounds(x, y, w, h);
//        new_btn.addActionListener(this);
        new_btn.setText(text);
        new_btn.setFocusable(false);
        new_btn.setFont(new Font("Consolas", Font.BOLD, 17));
        return new_btn;
    }

    private void updateTextArea(final String text, JTextArea textArea) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
//                textArea.setText(textArea.getText() + text);
                textArea.append(text);
            }
        });
    }

    private void redirectSystemStreams(JTextArea textArea) {
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                updateTextArea(String.valueOf((char) b), textArea);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                updateTextArea(new String(b, off, len), textArea);
            }

            @Override
            public void write(byte[] b) throws IOException {
                write(b, 0, b.length);
            }
        };

        System.setOut(new PrintStream(out, true));
//        System.setErr(new PrintStream(out, true));
    }

    private void replacePanel(JPanel next_panel){
        int index = this.getComponentZOrder(this.panel_up);
        this.remove(this.panel_up);
        this.panel_up = next_panel;
        this.add(this.panel_up, index);
        this.revalidate();
        this.repaint();
        this.setVisible(true);
    }
}
