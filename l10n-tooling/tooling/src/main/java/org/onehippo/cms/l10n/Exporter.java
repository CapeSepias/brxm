/*
 *  Copyright 2016-2016 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cms.l10n;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import static org.onehippo.cms.l10n.KeyData.KeyStatus.ADDED;
import static org.onehippo.cms.l10n.KeyData.KeyStatus.UPDATED;
import static org.onehippo.cms.l10n.KeyData.LocaleStatus.UNRESOLVED;

public class Exporter {

    private final File baseDir;
    private final String format;

    Exporter(final File baseDir, final String format) {
        this.baseDir = baseDir;
        this.format = format;
    }

    File export(final String locale) throws Exception {
        return export(locale, false);
    }

    File export(final String locale, final boolean full) throws Exception {
        final Collection<Translation> pending = new ArrayList<>();
        for (Module module : new ModuleLoader(baseDir).loadModules()) {
            final Registry registry = module.getRegistry();
            for (final RegistryInfo registryInfo : registry.getRegistryInfos()) {
                registryInfo.load();
                for (final String key : registryInfo.getKeys()) {
                    final KeyData data = registryInfo.getKeyData(key);
                    if (full) {
                        pending.add(new Translation(module, registryInfo, key, locale));
                    } else {
                        if (data.getStatus() == ADDED || data.getStatus() == UPDATED) {
                            if (data.getLocaleStatus(locale) == UNRESOLVED) {
                                pending.add(new Translation(module, registryInfo, key, locale));
                            }
                        }
                    }
                }
            }
        }
        final File csv = new File(baseDir, "export_" + locale + ".csv");
        try (final FileWriter writer = new FileWriter(csv)) {
            final String englishHeader = Locale.ENGLISH.getDisplayName(new Locale(locale));
            final String translationLanguageHeader = new Locale(locale).getDisplayName(new Locale(locale));
            final CSVFormat csvFormat = CSVFormat.valueOf(format).withHeader("Key", englishHeader, translationLanguageHeader);
            final CSVPrinter printer = new CSVPrinter(writer, csvFormat);
            for (final Translation translation : pending) {
                printer.printRecord(translation.getFQKey(), translation.getReferenceValue(), translation.getTranslation());
            }
        }
        return csv;
    }

    public static void main(String[] args) throws Exception {

        final Options options = new Options();
        final Option basedirOption = new Option("d", "basedir", true, "the project base directory");
        basedirOption.setRequired(true);
        options.addOption(basedirOption);
        final Option localeOption = new Option("l", "locale", true, "the locale to export");
        localeOption.setRequired(true);
        options.addOption(localeOption);
        final Option formatOption = new Option("f", "format", true, "the csv format");
        options.addOption(formatOption);
        final Option allOption = new Option("a", "all", true, "export all or only missing");
        options.addOption(allOption);

        final CommandLineParser parser = new DefaultParser();
        final CommandLine commandLine = parser.parse(options, args);
        final File baseDir = new File(commandLine.getOptionValue("basedir")).getCanonicalFile();
        final String locale = commandLine.getOptionValue("locale");
        final String format = commandLine.getOptionValue("format", "Default");
        final boolean all = commandLine.getOptionValue("all", "false").equals("true");

        System.out.println("exporting all: " + all + " " + commandLine.getOptionValue("all"));

        new Exporter(baseDir, format).export(locale, all);
    }
    
}
