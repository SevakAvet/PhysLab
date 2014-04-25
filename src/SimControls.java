import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;

class MyLabel extends JLabel {
    String sample = null;

    public MyLabel(String text) {
        super(text);
    }

    public MyLabel(String text, int alignment) {
        super(text, alignment);
    }

    public Dimension getPreferredSize() {
        Font myFont = new Font("SansSerif", Font.PLAIN, 12);
        this.setFont(myFont);
        FontMetrics myFM = this.getFontMetrics(myFont);
        int w, h;
        String txt;
        if (sample == null)
            txt = this.getText();
        else
            txt = sample;
        w = 5 + myFM.stringWidth(txt);
        h = myFM.getAscent() + myFM.getDescent();
        return new Dimension(w, h);
    }
}

class SimLine extends JComponent {
    public void paint(Graphics g) {
        Dimension size = getSize();
        g.setColor(Color.gray);
        g.fillRect(0, 0, size.width, size.height);
    }

    public Dimension getPreferredSize() {
        return new Dimension(1000, 1);
    }
}

class DoubleField extends JComponent implements Observer {
    private double value;
    private JTextField field;
    private MyLabel nameLabel;
    private NumberFormat nf;
    private NumberValidator validator;
    private Subject subj;
    private String name;

    public DoubleField(Subject subj, String name, double value,
                       int digits, int columns) {
        setLayout(new BorderLayout(1, 1));
        this.subj = subj;
        this.value = value;
        this.name = name;
        nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(digits);
        nf.setMinimumFractionDigits(digits);
        nameLabel = new MyLabel(name, SwingConstants.CENTER);
        add(nameLabel, BorderLayout.WEST);
        field = new JTextField(nf.format(value), columns);
        add(field, BorderLayout.EAST);
        validator = new NumberValidator(this);
        field.addActionListener(validator);
        field.addFocusListener(validator);
    }

    public DoubleField(Subject subj, String name, int digits) {
        this(subj, name, subj.getParameter(name), digits, 4);
    }

    public String toString() {
        return "DoubleField \"" + name + "\" value=" + nf.format(value);
    }

    public void update(Subject subj, String param, double value) {
        if (param.equalsIgnoreCase(this.name) && this.value != value) {
            this.value = value;
            field.setText(nf.format(value));
        }
    }

    private void revert() {
        field.setText(nf.format(value));
    }


    private void modifyValue(double value) {
        this.value = value;
        field.setText(nf.format(value));
        if (subj != null) {
            subj.setParameter(name, value);
        }
    }

    protected class NumberValidator extends FocusAdapter implements ActionListener {
        DoubleField dblField;

        protected NumberValidator(DoubleField dblField) {
            super();
            this.dblField = dblField;
        }

        public void actionPerformed(ActionEvent event) {
            validate((JTextField) event.getSource());
        }

        public void focusGained(FocusEvent event) {
            JTextField tf = (JTextField) event.getSource();
            tf.selectAll();
        }

        public void focusLost(FocusEvent event) {
            JTextField tf = (JTextField) event.getSource();
            validate(tf);
            tf.select(0, 0);
        }

        private void validate(JTextField field) {
            try {

                double value = new Double(field.getText());
                if (value != dblField.value)
                    dblField.modifyValue(value);
            } catch (NumberFormatException e) {
                dblField.revert();
            }
        }
    }
}