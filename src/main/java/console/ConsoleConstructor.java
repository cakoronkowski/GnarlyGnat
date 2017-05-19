package console;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintStream;


public class ConsoleConstructor extends JPanel {

    JTextArea textArea = new JTextArea(15, 30);

    ConsoleStream cStream = new ConsoleStream(
            textArea, "☠");

    public ConsoleConstructor() {
        setLayout(new BorderLayout());
        JScrollPane p= new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JScrollBar bar= new JScrollBar();
        bar.setUnitIncrement(40);
        bar.setBackground(Color.decode("#424242"));
        bar.setForeground(Color.decode("#E0E0E0"));
        bar.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));

        p.setVerticalScrollBar(bar);
        p.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));

        add(p);
        System.setOut(new PrintStream(cStream));
        System.setErr(new PrintStream(cStream));

    }

    public void clear()
    {
        textArea.setText("");
    }
}
