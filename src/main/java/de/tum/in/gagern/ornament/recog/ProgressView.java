package de.tum.in.gagern.ornament.recog;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

class ProgressView extends ProgressPhase implements Runnable, ActionListener {

    private static final int SCALE = (1 << 15);

    private final JDialog dlg;

    private final JProgressBar overallBar;

    private final JProgressBar taskBar;

    private final JLabel noticeLabel;

    private final JButton cancelButton;

    private boolean isCanceled;

    private boolean updateScheduled;

    private int overallValue;

    private int taskValue;

    private String noticeText;

    private float weightSum;

    public ProgressView(Component parent, String title, String cancelCaption) {
	super(null, 1, 0);

	isCanceled = false;
	updateScheduled = false;
	overallValue = 0;
	taskValue = 0;
	noticeText = "";
	weightSum = 0.f;

	Frame owner = JOptionPane.getFrameForComponent(parent);
	dlg = new JDialog(owner, title, false);
	overallBar = new JProgressBar(0, SCALE);
	taskBar = new JProgressBar(0, SCALE);
	noticeLabel = new JLabel(noticeText);
	cancelButton = new JButton(cancelCaption);
	cancelButton.addActionListener(this);

	GridBagConstraints gbc = new GridBagConstraints();
	gbc.insets = new Insets(5, 5, 5, 5);
	gbc.weightx = 1;
	gbc.fill = GridBagConstraints.HORIZONTAL;
	gbc.gridwidth = GridBagConstraints.REMAINDER;
	Container c = dlg.getContentPane();
	c.setLayout(new GridBagLayout());
	c.add(overallBar, gbc);
	c.add(noticeLabel, gbc);
	c.add(taskBar, gbc);
	gbc.fill = GridBagConstraints.NONE;
	gbc.anchor = GridBagConstraints.LINE_END;
	c.add(cancelButton, gbc);
    }

    public synchronized void update(float newValue, ProgressPhase activeChild)
	throws CanceledOperationException {
	super.update(newValue, activeChild);
	if (isCanceled) throw new CanceledOperationException();
	overallValue = (int)Math.floor(SCALE*getFraction());
	taskValue = (int)Math.floor(SCALE*activeChild.getFraction());
	if (!activeChild.isDeterminate())
	    taskValue = -1;
	scheduleUpdate();
    }

    public synchronized void setNotice(String notice) {
	super.setNotice(notice);
	noticeText = (notice == null ? "" : notice);
	scheduleUpdate();
    }

    private synchronized void scheduleUpdate() {
	if (EventQueue.isDispatchThread()) {
	    run();
	}
	else if (!updateScheduled) {
	    updateScheduled = true;
	    EventQueue.invokeLater(this);
	}
    }

    public void show() {
	EventQueue.invokeLater(new Runnable(){ public void run() {
	    String text = noticeLabel.getText();
	    noticeLabel.setText("Some rather long text for minimum size");
	    dlg.pack();
	    noticeLabel.setMinimumSize(noticeLabel.getSize());
	    noticeLabel.setText(text);
	    dlg.setLocationRelativeTo(dlg.getOwner());
	    dlg.setVisible(true);
	}});
    }

    public void close() {
	EventQueue.invokeLater(new Runnable(){ public void run() {
	    dlg.setVisible(false);
	    dlg.dispose();
	}});
    }

    public synchronized void run() {
	updateScheduled = false;
	overallBar.setValue(overallValue);
	if (taskValue < 0) {
	    taskBar.setIndeterminate(true);
	    taskBar.setValue(0);
	}
	else {
	    taskBar.setValue(taskValue);
	    taskBar.setIndeterminate(false);
	}
	noticeLabel.setText(noticeText);
	cancelButton.setEnabled(!isCanceled);
    }

    public synchronized void actionPerformed(ActionEvent evnt) {
	isCanceled = true;
	cancelButton.setEnabled(false);
    }

    public Component getComponent() {
	return dlg;
    }

}
