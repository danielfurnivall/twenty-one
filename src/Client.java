import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

public class Client{


    public static void main(String[] args) throws IOException, InterruptedException {
        String name;
        try{
            Socket connection = new Socket("127.0.0.1", 6969);

            String line;


// Ugly swing stuff
            JFrame frame = new JFrame("Twenty One - Client");
            JTextArea textPuller = new JTextArea();
            textPuller.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(textPuller);
            JPanel middlePanel=new JPanel();
            middlePanel.setBorder(new TitledBorder(new EtchedBorder(), "Console"));
            JPanel bottomPanel = new JPanel();
            JTextField input = new JTextField(15);
            JButton stick = new JButton("Stick");
            JButton draw = new JButton("Draw");
            JButton quit = new JButton("Quit");


            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            middlePanel.add(scrollPane, BorderLayout.NORTH);
            bottomPanel.add(input, BorderLayout.SOUTH);




            JButton submitName = new JButton("Submit name");
            bottomPanel.add(stick, BorderLayout.SOUTH);
            bottomPanel.add(draw, BorderLayout.NORTH);
            bottomPanel.add(quit, BorderLayout.CENTER);
            bottomPanel.add(submitName, BorderLayout.NORTH);

            scrollPane.setPreferredSize(new Dimension(475, 400));
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            frame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);
            frame.getContentPane().add(middlePanel, BorderLayout.CENTER);


//actionlistener for draw card button
            draw.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {

                    try{
                        PrintWriter writer = new PrintWriter(connection.getOutputStream(),true);
                        writer.write("d\n");
                        writer.flush();
                    }catch(IOException e){
                        e.printStackTrace();
                    }
                }
            });

            //actionlistener for stick button
            stick.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    try{
                        PrintWriter writer = new PrintWriter(connection.getOutputStream(),true);
                        writer.write("s\n");
                        writer.flush();
                    }catch(IOException e){
                        e.printStackTrace();
                    }
                }
            });


            //actionlistener for submit name button
            submitName.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    try{
                        PrintWriter writer = new PrintWriter(connection.getOutputStream(),true);
                        textPuller.setCaretPosition(textPuller.getDocument().getLength());
                        writer.println(input.getText());


                    }catch(IOException e){
                        e.printStackTrace();
                    }

                }
            });
            quit.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {

                    System.exit(1);

                }
            });
            frame.pack();
            scrollPane.setSize(400, 400);
            frame.setSize(500, 500);
            frame.setVisible(true);
            middlePanel.setVisible(true);
            textPuller.setVisible(true);

            stick.setVisible(false);
            draw.setVisible(false);
            quit.setVisible(false);





            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            //Show welcome message from server
            textPuller.append("---------Welcome to Danny's 21 Server!---------");
            textPuller.append("\n---------Please enter your name---------");


            int nametries=0;
            while (true) {

                nametries++;
                //loop to get name input
                while (true) {
                    line = reader.readLine();

                    //if server sends "newName" (i.e. not in player list hashmap) then break out
                    if (line.equals("newName")) {
                        line ="";
                        name = input.getText();
                        input.setVisible(false);

                        break;
                    }

                    if (line.equals("Name Taken")) {
                        textPuller.append("\nName already taken, please enter another.");
                        textPuller.setCaretPosition(textPuller.getDocument().getLength());

                    }
                    if (line.equals("wrong format")){
                        textPuller.append("\nUnacceptable name format (must be alphabetical)." +
                                " Please enter another.");
                        textPuller.setCaretPosition(textPuller.getDocument().getLength());
                    }
                }

                //Entry to the game.
                textPuller.append("\nHi, " + name + "! Welcome to the game :)");
                textPuller.setCaretPosition(textPuller.getDocument().getLength());
                submitName.setVisible(false);
                quit.setVisible(false);
                middlePanel.setBorder(new TitledBorder(new EtchedBorder(), name));

                while ((line = reader.readLine()) != null) {
                    stick.setVisible(false);
                    draw.setVisible(false);
                    textPuller.append("\n" + line);
                    textPuller.setCaretPosition(textPuller.getDocument().getLength());//this is to ensure autoscrolling
                    Thread.sleep(10); //I put this delay in to make the text flow a little slower to help with readability

                    if (line.equals("Stick (s) or draw new card (d)?")) {

                        stick.setVisible(true);
                        draw.setVisible(true);
                    }
                    if (line.equals("You may now close this window.")){
                        quit.setVisible(true);
                    }


                }
            }
        }catch(IOException | NullPointerException e){
            e.printStackTrace();
        }
    }

}
