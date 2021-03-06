/*
 *  This file is part of Player Analytics (Plan).
 *
 *  Plan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License v3 as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Plan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Plan. If not, see <https://www.gnu.org/licenses/>.
 */
package com.djrapitops.plan.system;

import com.djrapitops.plan.utilities.formatting.Formatters;
import com.djrapitops.plan.utilities.html.graphs.Graphs;
import com.djrapitops.plan.utilities.html.structure.Accordions;
import com.djrapitops.plan.utilities.html.structure.AnalysisPluginsTabContentCreator;
import com.djrapitops.plan.utilities.html.tables.HtmlTables;
import dagger.Lazy;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class HtmlUtilities {

    private final Lazy<Formatters> formatters;
    private final Lazy<HtmlTables> htmlTables;
    private final Lazy<Graphs> graphs;
    private final Lazy<Accordions> accordions;
    private final Lazy<AnalysisPluginsTabContentCreator> analysisPluginsTabContentCreator;

    @Inject
    public HtmlUtilities(
            Lazy<Formatters> formatters,
            Lazy<HtmlTables> htmlTables,
            Lazy<Graphs> graphs,
            Lazy<Accordions> accordions,
            Lazy<AnalysisPluginsTabContentCreator> analysisPluginsTabContentCreator
    ) {
        this.formatters = formatters;
        this.htmlTables = htmlTables;
        this.graphs = graphs;
        this.accordions = accordions;
        this.analysisPluginsTabContentCreator = analysisPluginsTabContentCreator;
    }

    public Formatters getFormatters() {
        return formatters.get();
    }

    public HtmlTables getHtmlTables() {
        return htmlTables.get();
    }

    public Graphs getGraphs() {
        return graphs.get();
    }

    public Accordions getAccordions() {
        return accordions.get();
    }

    public AnalysisPluginsTabContentCreator getAnalysisPluginsTabContentCreator() {
        return analysisPluginsTabContentCreator.get();
    }
}
