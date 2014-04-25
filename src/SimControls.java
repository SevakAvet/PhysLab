import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;



class MyScrollbar extends JScrollBar {
    int w, h;

    public MyScrollbar(int w, int h, int orient, int value, int vis, int min, int max) {

        super(orient, value, vis, min, max);
        this.w = w;
        this.h = h;
    }

    public Dimension getPreferredSize() {
        return new Dimension(w, h);
    }
}





class MyLabel extends JLabel {
    String sample = null;

    public MyLabel(String text) {
        super(text);
    }

    public MyLabel(String text, int alignment) {
        super(text, alignment);
    }

    public MyLabel(String text, int alignment, String sample) {
        super(text, alignment);
        this.sample = sample;
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



class MyCheckbox extends JCheckBox implements ItemListener, Observer {
    private double value;
    private Subject subj;
    private String name;

    public MyCheckbox(Subject subj, String name) {
        super(name);
        this.subj = subj;
        this.name = name;
        this.value = subj.getParameter(name);
        setSelected(this.value != 0);
        addItemListener(this);
    }

    public String toString() {
        return "MyCheckbox \"" + name + "\" value=" + value;
    }

    public void update(Subject subj, String param, double value) {
        if (param.equalsIgnoreCase(name) && value != this.value) {
            this.value = value;
            setSelected(this.value != 0);
        }
    }

    public void itemStateChanged(ItemEvent event) {
        ItemSelectable isl = event.getItemSelectable();
        if (isl == this) {
            value = (null != getSelectedObjects()) ? 1 : 0;
            subj.setParameter(name, value);
        }
    }
}



class MySlider extends JComponent implements AdjustmentListener, Observer {
    private double min, delta, value;
    private MyScrollbar scroll;
    private MyLabel nameLabel;
    private MyLabel myNumber;
    private NumberFormat nf;
    private Subject subj;
    private String name;

    public MySlider(Subject subj, String name,
                    double min, double max, int increments, int digits) {
        this.subj = subj;
        this.name = name;
        this.min = min;
        this.value = subj.getParameter(name);
        delta = (max - min) / increments;
        nameLabel = new MyLabel(name, SwingConstants.CENTER);
        add(nameLabel);

        scroll = new MyScrollbar(75, 15, Scrollbar.HORIZONTAL, (int) (0.5 + (value - min) / delta),
                10, 0, increments + 10);
        add(scroll);
        scroll.addAdjustmentListener(this);
        nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(digits);
        nf.setMinimumFractionDigits(digits);
        myNumber = new MyLabel(nf.format(value), SwingConstants.LEFT, "88.88");
        add(myNumber);
        setLayout(new FlowLayout(FlowLayout.CENTER, 1, 1));
    }

    public String toString() {
        return "MySlider \"" + name + "\" value=" + nf.format(value);
    }

    public void update(Subject subj, String param, double value) {
        if (param.equalsIgnoreCase(name) && value != this.value) {
            this.value = value;
            myNumber.setText(nf.format(value));


            scroll.setValue((int) (0.5 + (value - min) / delta));
        }
    }



    public void adjustmentValueChanged(AdjustmentEvent e) {
        if (e.getAdjustable() == scroll) {
            value = min + (double) scroll.getValue() * delta;
            myNumber.setText(nf.format(value));
            if (subj != null)
                subj.setParameter(name, value);
        }
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

    public DoubleField(Subject subj, String name, double value, int digits) {
        this(subj, name, value, digits, 4);
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

                double value = (new Double(field.getText())).doubleValue();
                if (value != dblField.value)
                    dblField.modifyValue(value);
            } catch (NumberFormatException e) {
                dblField.revert();
            }
        }
    }
}






class MyChoice extends JComboBox implements ItemListener, Observer {
    private double value;
    private double min;
    private String name;
    private Subject subj;

    public MyChoice(Subject subj, String name, double value, double min, Object[] choices) {
        this.subj = subj;
        this.name = name;
        this.value = value;
        this.min = min;
        int index = (int) (value - min);
        if (index < 0 || index >= choices.length)
            throw new IllegalArgumentException("Value=" + value + " but must be in range " + min +
                    " to " + (min + choices.length - 1));
        for (int i = 0; i < choices.length; i++)
            addItem(choices[i].toString());
        setSelectedIndex(index);
        addItemListener(this);
    }

    public void itemStateChanged(ItemEvent e) {
        value = min + (double) getSelectedIndex();
        if (subj != null)
            subj.setParameter(name, value);
    }

    public void update(Subject subj, String param, double value) {
        if (param.equalsIgnoreCase(name) && value != this.value) {
            int index = (int) (Math.floor(value) - min);
            if (index < 0 || index >= this.getItemCount())
                throw new IllegalArgumentException("Value=" + value + " but must be in range " + min +
                        " to " + (min + getItemCount() - 1));
            this.value = Math.floor(value);
            setSelectedIndex((int) (this.value - this.min));
        }
    }
}

