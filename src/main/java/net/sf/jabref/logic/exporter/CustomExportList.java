/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.logic.exporter;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import net.sf.jabref.logic.journals.JournalAbbreviationLoader;
import net.sf.jabref.logic.layout.LayoutFormatterPreferences;
import net.sf.jabref.preferences.JabRefPreferences;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class handles user defined custom export formats. They are initially
 * read from Preferences, and kept alphabetically (sorted by name). Formats can
 * be added or removed. When modified, the sort() method must be called to make
 * sure the formats stay properly sorted. When the method store() is called,
 * export formats are written to Preferences.
 */

public class CustomExportList {

    private final EventList<List<String>> list;
    private final SortedList<List<String>> sorted;
    private final Map<String, ExportFormat> formats = new TreeMap<>();

    private static final Log LOGGER = LogFactory.getLog(CustomExportList.class);


    public CustomExportList(Comparator<List<String>> comp) {
        list = new BasicEventList<>();
        sorted = new SortedList<>(list, comp);
    }

    public Map<String, ExportFormat> getCustomExportFormats(JabRefPreferences prefs,
            JournalAbbreviationLoader loader) {
        formats.clear();
        readPrefs(prefs, loader);
        return formats;
    }

    public int size() {
        return list.size();
    }

    public EventList<List<String>> getSortedList() {
        return sorted;
    }

    private void readPrefs(JabRefPreferences prefs, JournalAbbreviationLoader loader) {
        formats.clear();
        list.clear();
        int i = 0;
        List<String> s;
        LayoutFormatterPreferences layoutPreferences = LayoutFormatterPreferences.fromPreferences(prefs, loader);
        SavePreferences savePreferences = SavePreferences.loadForExportFromPreferences(prefs);
        while (!((s = prefs.getStringList(JabRefPreferences.CUSTOM_EXPORT_FORMAT + i)).isEmpty())) {
            Optional<ExportFormat> format = createFormat(s, layoutPreferences, savePreferences);
            if (format.isPresent()) {
                formats.put(format.get().getConsoleName(), format.get());
                list.add(s);
            } else {
                String customExportFormat = prefs.get(JabRefPreferences.CUSTOM_EXPORT_FORMAT + i);
                LOGGER.error("Error initializing custom export format from string " + customExportFormat);
            }
            i++;
        }
    }

    private Optional<ExportFormat> createFormat(List<String> s, LayoutFormatterPreferences layoutPreferences,
            SavePreferences savePreferences) {
        if (s.size() < 3) {
            return Optional.empty();
        }
        String lfFileName;
        if (s.get(1).endsWith(".layout")) {
            lfFileName = s.get(1).substring(0, s.get(1).length() - 7);
        } else {
            lfFileName = s.get(1);
        }
        ExportFormat format = new ExportFormat(s.get(0), s.get(0), lfFileName, null, s.get(2), layoutPreferences,
                savePreferences);
        format.setCustomExport(true);
        return Optional.of(format);
    }

    public void addFormat(List<String> s, LayoutFormatterPreferences layoutPreferences, SavePreferences savePreferences) {
        createFormat(s, layoutPreferences, savePreferences).ifPresent(format -> {
            formats.put(format.getConsoleName(), format);
            list.add(s);
        });
    }

    public void remove(List<String> toRemove, LayoutFormatterPreferences layoutPreferences,
            SavePreferences savePreferences) {
        createFormat(toRemove, layoutPreferences, savePreferences).ifPresent(format -> {
            formats.remove(format.getConsoleName());
            list.remove(toRemove);
        });
    }

    public void store(JabRefPreferences prefs) {

        if (list.isEmpty()) {
            purge(0, prefs);
        } else {
            for (int i = 0; i < list.size(); i++) {
                prefs.putStringList(JabRefPreferences.CUSTOM_EXPORT_FORMAT + i, list.get(i));
            }
            purge(list.size(), prefs);
        }
    }

    private void purge(int from, JabRefPreferences prefs) {
        int i = from;
        while (!prefs.getStringList(JabRefPreferences.CUSTOM_EXPORT_FORMAT + i).isEmpty()) {
            prefs.remove(JabRefPreferences.CUSTOM_EXPORT_FORMAT + i);
            i++;
        }
    }

}
