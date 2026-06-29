package edu.gkg.view;

import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoSuggestTextFieldTest {

    @Test
    void selectingSuggestionFillsTextField() throws Exception {
        AutoSuggestTextField field = new AutoSuggestTextField(prefix -> List.of("New York", "New Delhi"));

        SwingUtilities.invokeAndWait(() -> {
            field.setText("new");
            field.showSuggestionsForTests();
            field.selectSuggestionForTests("New York");
        });

        assertEquals("New York", field.getText());
    }
}
