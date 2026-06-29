package edu.gkg.view;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Function;

public class AutoSuggestTextField extends JTextField {

    private static final int DEFAULT_LIMIT = 10;

    private Function<String, List<String>> suggestionProvider;
    private final JPopupMenu popup = new JPopupMenu();
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> suggestionList = new JList<>(listModel);
    private boolean selecting;
    private SwingWorker<List<String>, Void> worker;

    public AutoSuggestTextField() {
        this(prefix -> List.of());
    }

    public AutoSuggestTextField(Function<String, List<String>> suggestionProvider) {
        super();
        this.suggestionProvider = suggestionProvider == null ? prefix -> List.of() : suggestionProvider;
        configurePopup();
        wire();
    }

    public void setSuggestionProvider(Function<String, List<String>> suggestionProvider) {
        this.suggestionProvider = suggestionProvider == null ? prefix -> List.of() : suggestionProvider;
        hideSuggestions();
    }

    private void configurePopup() {
        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionList.setVisibleRowCount(DEFAULT_LIMIT);
        suggestionList.setFocusable(false);
        popup.setFocusable(false);
        popup.add(new JScrollPane(suggestionList));
    }

    private void wire() {
        getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refreshSuggestions(); }
            @Override public void removeUpdate(DocumentEvent e) { refreshSuggestions(); }
            @Override public void changedUpdate(DocumentEvent e) { refreshSuggestions(); }
        });

        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (!popup.isVisible()) return;
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    moveSelection(1);
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    moveSelection(-1);
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    applySelectedSuggestion();
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    hideSuggestions();
                    e.consume();
                }
            }
        });

        suggestionList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 1) applySelectedSuggestion();
            }
        });
    }

    private void refreshSuggestions() {
        if (selecting) return;
        String prefix = getText().trim();
        if (prefix.isEmpty()) {
            hideSuggestions();
            return;
        }
        if (worker != null) worker.cancel(true);
        worker = new SwingWorker<>() {
            @Override protected List<String> doInBackground() {
                return suggestionProvider.apply(prefix);
            }

            @Override protected void done() {
                if (isCancelled()) return;
                try {
                    showSuggestions(get());
                } catch (Exception ignored) {
                    hideSuggestions();
                }
            }
        };
        worker.execute();
    }

    private void showSuggestions(List<String> suggestions) {
        if (suggestions == null || suggestions.isEmpty() || !isShowing()) {
            hideSuggestions();
            return;
        }
        populateSuggestions(suggestions);
        if (listModel.isEmpty()) {
            hideSuggestions();
            return;
        }
        suggestionList.setSelectedIndex(0);
        suggestionList.setFixedCellWidth(Math.max(getWidth(), 240));
        popup.setPreferredSize(new Dimension(Math.max(getWidth(), 240),
                Math.min(220, listModel.size() * 28 + 8)));
        popup.show(this, 0, getHeight());
    }

    private void populateSuggestions(List<String> suggestions) {
        listModel.clear();
        suggestions.stream()
                .filter(value -> value != null && !value.isBlank())
                .limit(DEFAULT_LIMIT)
                .forEach(listModel::addElement);
    }

    private void moveSelection(int delta) {
        if (listModel.isEmpty()) return;
        int next = Math.max(0, Math.min(listModel.size() - 1, suggestionList.getSelectedIndex() + delta));
        suggestionList.setSelectedIndex(next);
        suggestionList.ensureIndexIsVisible(next);
    }

    private void applySelectedSuggestion() {
        String value = suggestionList.getSelectedValue();
        if (value == null) return;
        selecting = true;
        setText(value);
        setCaretPosition(value.length());
        selecting = false;
        hideSuggestions();
    }

    private void hideSuggestions() {
        popup.setVisible(false);
    }

    void showSuggestionsForTests() {
        populateSuggestions(suggestionProvider.apply(getText().trim()));
    }

    void selectSuggestionForTests(String value) {
        for (int i = 0; i < listModel.size(); i++) {
            if (listModel.get(i).equals(value)) {
                suggestionList.setSelectedIndex(i);
                applySelectedSuggestion();
                return;
            }
        }
    }
}
