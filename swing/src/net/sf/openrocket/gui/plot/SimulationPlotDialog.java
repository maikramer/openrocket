package net.sf.openrocket.gui.plot;

import net.miginfocom.swing.MigLayout;
import net.sf.openrocket.document.Simulation;
import net.sf.openrocket.gui.components.StyledLabel;
import net.sf.openrocket.gui.util.GUIUtil;
import net.sf.openrocket.gui.util.Icons;
import net.sf.openrocket.l10n.Translator;
import net.sf.openrocket.startup.Application;
import net.sf.openrocket.startup.Preferences;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.util.ArrayList;

/**
 * Dialog that shows a plot of a simulation results based on user options.
 *
 * @author Sampo Niskanen <sampo.niskanen@iki.fi>
 */
public class SimulationPlotDialog extends JDialog {

    private static final Translator trans = Application.getTranslator();

    private SimulationPlotDialog(Window parent, Simulation simulation, PlotConfiguration config) {
        //// Flight data plot
        super(parent, simulation.getName());
        this.setModalityType(ModalityType.DOCUMENT_MODAL);

        final boolean initialShowPoints = Application.getPreferences().getBoolean(Preferences.PLOT_SHOW_POINTS, false);

        final SimulationPlot myPlot = new SimulationPlot(simulation, config, initialShowPoints);

        // Create the dialog
        JPanel panel = new JPanel(new MigLayout("fill", "[]", "[grow][]"));
        this.add(panel);

        final ChartPanel chartPanel = new SimulationChart(myPlot.getJFreeChart());
        chartPanel.setMouseZoomable(true);
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.setDomainZoomable(true);
        chartPanel.setRangeZoomable(false);
        chartPanel.setZoomTriggerDistance(20);
        chartPanel.setZoomOutlinePaint(new Color(0f, 0f, 0f, 0f));
        chartPanel.setZoomAroundAnchor(true);
        chartPanel.setPanWithMiddleMouse(true);
        chartPanel.addMouseWheelListener(arg0 -> chartPanel.restoreAutoRangeBounds());

        panel.add(chartPanel, "grow, wrap 20lp");

        //// Description text
        JLabel label = new StyledLabel(trans.get("PlotDialog.lbl.Chart"), -2);
        panel.add(label, "wrap");

        //// Show data points
        final JCheckBox check = new JCheckBox(trans.get("PlotDialog.CheckBox.Showdatapoints"));
        check.setSelected(initialShowPoints);
        check.addActionListener(e -> {
            boolean show = check.isSelected();
            Application.getPreferences().putBoolean(Preferences.PLOT_SHOW_POINTS, show);
            myPlot.setShowPoints(show);
        });
        panel.add(check, "split, left");

        //// Zoom in button
        JButton button = new JButton(Icons.ZOOM_IN);
        button.addActionListener(e -> {
            if ((e.getModifiers() & InputEvent.ALT_DOWN_MASK) == InputEvent.ALT_DOWN_MASK) {
                chartPanel.actionPerformed(new ActionEvent(chartPanel, ActionEvent.ACTION_FIRST, ChartPanel.ZOOM_IN_DOMAIN_COMMAND));
            } else {
                chartPanel.actionPerformed(new ActionEvent(chartPanel, ActionEvent.ACTION_FIRST, ChartPanel.ZOOM_IN_BOTH_COMMAND));

            }
        });
        panel.add(button, "gapleft rel");

        //// Reset Zoom button.
        button = new JButton(Icons.ZOOM_RESET);
        button.addActionListener(e -> chartPanel.actionPerformed(new ActionEvent(chartPanel, ActionEvent.ACTION_FIRST, ChartPanel.ZOOM_RESET_BOTH_COMMAND)));
        panel.add(button, "gapleft rel");


        //// Zoom out button
        button = new JButton(Icons.ZOOM_OUT);
        button.addActionListener(e -> {
            if ((e.getModifiers() & InputEvent.ALT_DOWN_MASK) == InputEvent.ALT_DOWN_MASK) {
                chartPanel.actionPerformed(new ActionEvent(chartPanel, ActionEvent.ACTION_FIRST, ChartPanel.ZOOM_OUT_DOMAIN_COMMAND));
            } else {
                chartPanel.actionPerformed(new ActionEvent(chartPanel, ActionEvent.ACTION_FIRST, ChartPanel.ZOOM_OUT_BOTH_COMMAND));
            }
        });
        panel.add(button, "gapleft rel");

        //// Add series selection box
        ArrayList<String> stages = new ArrayList<>();
        stages.add("All");
        stages.addAll(Util.generateSeriesLabels(simulation));

        final JComboBox<?> stageSelection = new JComboBox<>(stages.toArray(new String[0]));
        stageSelection.addItemListener(e -> {
            int selectedStage = stageSelection.getSelectedIndex() - 1;
            myPlot.setShowBranch(selectedStage);
        });
        if (stages.size() > 2) {
            // Only show the combo box if there are at least 3 entries (ie, "All", "Main", and one other one
            panel.add(stageSelection, "gapleft rel");
        }
        JLabel crossHairLabel = new StyledLabel("", 2, 0, StyledLabel.Style.BOLD);
        ((XYPlot<?>) myPlot.getJFreeChart().getPlot()).addCrossHairChangeEventListener(event -> {
            String finalText = getCrossHairText(config, myPlot, event.getCrossHairState());
            if (finalText == null) {
                crossHairLabel.setText("");
                return;
            }
            crossHairLabel.setText(finalText);
        });

        panel.add(crossHairLabel);
        //// Spacer for layout to push close button to the right.
        panel.add(new JPanel(), "growx");

        //// Close button
        button = new JButton(trans.get("dlg.but.close"));
        button.addActionListener(e -> SimulationPlotDialog.this.dispose());
        panel.add(button, "right");

        this.setLocationByPlatform(true);
        this.pack();

        GUIUtil.setDisposableDialogOptions(this, button);
        GUIUtil.rememberWindowSize(this);
    }

    private String getCrossHairText(PlotConfiguration config, SimulationPlot myPlot, CrosshairState crossHairState) {
        XYPlot<?> plot = (XYPlot<?>) myPlot.getJFreeChart().getPlot();
        var domainValue = plot.getDomainCrosshairValue();
        StringBuilder finalText = new StringBuilder();
        if (domainValue < Double.MIN_VALUE || crossHairState == null) {
            return null;
        }

        var dataset = (XYSeriesCollection<?>) plot.getDataset(crossHairState.getDatasetIndex());
        var seriesCount = dataset.getSeriesCount();
        ArrayList<Integer> itemIndexes = new ArrayList<>(seriesCount);

        //Search for index
        for (int i = 0; i < seriesCount; i++) {
            XYSeries<?> serie = ((XYSeriesCollection<?>) dataset).getSeries(i);
            for (int j = 0; j < serie.getItemCount(); j++) {
                if (serie.getX(j).doubleValue() > domainValue) {
                    itemIndexes.add(i, j);
                    break;
                } else if (j == serie.getItemCount() - 1) {
                    itemIndexes.add(i, -1);
                }
            }
        }
        //Domain
        String domainValueStr = String.format("%.1f", domainValue);
        String domainLabel = (config.getDomainAxisType() + " (" + config.getDomainAxisUnit() + "): " + domainValueStr);
        finalText.append(domainLabel);

        // Compose text
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            if (itemIndexes.get(i) == -1) break;
            finalText.append("     ");

            var serie = dataset.getSeries(i);
            String value = String.format("%.1f", serie.getY(itemIndexes.get(i)).doubleValue());
            String label = (serie.getDescription() + ": " + value);
            finalText.append(label);
        }

        return finalText.toString();
    }


    /**
     * Static method that shows a plot with the specified parameters.
     *
     * @param parent     the parent window, which will be blocked.
     * @param simulation the simulation to plot.
     * @param config     the configuration of the plot.
     */
    public static SimulationPlotDialog getPlot(Window parent, Simulation simulation, PlotConfiguration config) {
        return new SimulationPlotDialog(parent, simulation, config);
    }

}
