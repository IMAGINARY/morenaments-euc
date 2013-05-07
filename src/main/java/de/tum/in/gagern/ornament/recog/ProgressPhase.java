package de.tum.in.gagern.ornament.recog;

import java.awt.Component;

class ProgressPhase {

    private final ProgressPhase parent;

    private ProgressPhase activeChild;

    private float current;

    protected float maximum;

    private float offset;

    private float weight;

    private String noticePrefix;

    private String noticeSuffix;

    protected ProgressPhase(ProgressPhase parent, float weight, float offset) {
	this.parent = parent;
	this.current = 0;
	this.maximum = 0;
	this.offset = offset;
	this.weight = weight;
    }

    public ProgressPhase createPhase(float weight) {
	ProgressPhase phase = new ProgressPhase(this, weight, maximum);
	maximum += weight;
	return phase;
    }

    public ProgressPhase[] createPhases(float[] weights) {
	ProgressPhase[] phases = new ProgressPhase[weights.length];
	for (int i = 0; i < weights.length; ++i)
	    phases[i] = createPhase(weights[i]);
	return phases;
    }

    public ProgressPhase[] createPhases(int count, float weight) {
	ProgressPhase[] phases = new ProgressPhase[count];
	for (int i = 0; i < count; ++i)
	    phases[i] = createPhase(weight);
	return phases;
    }

    public ProgressPhase[] createPhases(int count) {
	return createPhases(count, 1);
    }

    public void setMaximum(float maximum) {
	this.maximum = maximum;
    }

    public void step() throws CanceledOperationException {
	step(1);
    }

    public void step(float increment) throws CanceledOperationException {
	update (current + increment);
    }

    public void update(float newValue) throws CanceledOperationException {
	update(newValue, null);
    }

    public void update(float newValue, ProgressPhase activeChild)
	throws CanceledOperationException {
	this.activeChild = activeChild;
	current = newValue;
	if (maximum > 0 && current > maximum)
	    current = maximum;
	if (parent != null)
	    parent.update(offset + weight*getFraction(), this);
    }

    public float getFraction() {
	if (maximum <= 0) return 0;
	else return current / maximum;
    }

    public boolean isDeterminate() {
	return maximum > 0;
    }

    public void begin(String notice, float maximum)
	throws CanceledOperationException {
	this.maximum = maximum;
	update(0);
	if (notice != null) setNotice(notice);
    }

    public void begin(float maximum)
	throws CanceledOperationException {
	begin(null, maximum);
    }

    public void begin(String notice)
	throws CanceledOperationException {
	if (maximum == 0) maximum = weight;
	update(0);
	if (notice != null) setNotice(notice);
    }

    public void begin()
	throws CanceledOperationException {
	begin(null);
    }

    public void done()
	throws CanceledOperationException {
	if (maximum <= 0) maximum = 1;
	update(maximum);
	// setNotice(null);
    }

    public void setNotice(String notice) {
	if (parent == null) return;
	if (notice == null) {
	    parent.setNotice(null);
	}
	else {
	    if (noticePrefix != null) notice = noticePrefix + notice;
	    if (noticeSuffix != null) notice = notice + noticeSuffix;
	    parent.setNotice(notice);
	}
    }

    public void setNoticePrefix(String prefix) {
	noticePrefix = prefix;
    }

    public void setNoticeSuffix(String suffix) {
	noticeSuffix = suffix;
    }

    public Component getComponent() {
	if (parent == null) return null;
	return parent.getComponent();
    }

}
