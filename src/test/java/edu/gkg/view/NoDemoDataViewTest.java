package edu.gkg.view;

import edu.gkg.view.chart.CooccurNetworkPanel;
import edu.gkg.view.chart.SentimentDashboard;
import edu.gkg.view.chart.ThemeHeatmap;
import edu.gkg.view.chart.TrendLineChart;
import org.junit.jupiter.api.Test;

import javax.swing.JSplitPane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class NoDemoDataViewTest {

    @Test
    void queryAndAnalysisInputsStartBlank() {
        QueryPanel queryPanel = new QueryPanel();
        AnalysisPanel analysisPanel = new AnalysisPanel();
        ThemeTrackPanel themeTrackPanel = new ThemeTrackPanel();

        assertEquals("", queryPanel.dateFromField.getText());
        assertEquals("", queryPanel.dateToField.getText());
        assertEquals("", queryPanel.personSearchField.getText());
        assertEquals("", queryPanel.orgSearchField.getText());
        assertEquals("", analysisPanel.entityNameField.getText());
        assertEquals("", themeTrackPanel.themeCodeField.getText());
        assertEquals("", themeTrackPanel.fromDateField.getText());
        assertEquals("", themeTrackPanel.toDateField.getText());
    }

    @Test
    void chartComponentsStartWithoutDemoRenderChildren() {
        TrendLineChart trendLineChart = new TrendLineChart();
        ThemeHeatmap themeHeatmap = new ThemeHeatmap();
        CooccurNetworkPanel networkPanel = new CooccurNetworkPanel();
        SentimentDashboard sentimentDashboard = new SentimentDashboard();

        assertEquals(1, trendLineChart.getComponentCount());
        assertEquals(0, themeHeatmap.getComponentCount());
        assertEquals(0, networkPanel.getComponentCount());
        assertEquals(1, sentimentDashboard.getComponentCount());
    }

    @Test
    void importPanelUsesSingleColumnWorkspaceLayout() {
        ImportPanel importPanel = new ImportPanel();

        assertFalse(containsSplitPane(importPanel));
    }

    private static boolean containsSplitPane(java.awt.Container container) {
        for (java.awt.Component component : container.getComponents()) {
            if (component instanceof JSplitPane) return true;
            if (component instanceof java.awt.Container child && containsSplitPane(child)) return true;
        }
        return false;
    }
}
