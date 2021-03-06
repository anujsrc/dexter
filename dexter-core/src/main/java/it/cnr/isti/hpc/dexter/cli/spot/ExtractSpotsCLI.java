/**
 *  Copyright 2011 Diego Ceccarelli
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package it.cnr.isti.hpc.dexter.cli.spot;

import it.cnr.isti.hpc.cli.AbstractCommandLineInterface;
import it.cnr.isti.hpc.dexter.cli.label.ExportArticlesIdCLI;
import it.cnr.isti.hpc.dexter.label.IdHelper;
import it.cnr.isti.hpc.dexter.label.IdHelperFactory;
import it.cnr.isti.hpc.dexter.spot.clean.SpotManager;
import it.cnr.isti.hpc.io.reader.JsonRecordParser;
import it.cnr.isti.hpc.io.reader.RecordReader;
import it.cnr.isti.hpc.log.ProgressLogger;
import it.cnr.isti.hpc.property.ProjectProperties;
import it.cnr.isti.hpc.wikipedia.article.Article;
import it.cnr.isti.hpc.wikipedia.article.Link;
import it.cnr.isti.hpc.wikipedia.reader.filter.TypeFilter;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retrieves all the titles and anchors from the Wikipedia articles, considers
 * only articles, redirects, templates and categories. The output file contains
 * the fields: <br>
 * <br>
 * {@code spot <tab> source id (id article containing the spot) <tab> target id (id of the target) article }
 * <br>
 * <br>
 * In case of a redirect or a title the source id is equal to the target id.
 * Each spot is processed using the {@link SpotManager#getStandardSpotManager()
 * standard spot manager}, which cleans, enriches and filters the text.
 * 
 */
public class ExtractSpotsCLI extends AbstractCommandLineInterface {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = LoggerFactory
			.getLogger(ExtractSpotsCLI.class);

	private static String[] params = new String[] { INPUT, OUTPUT };

	private static final String USAGE = "java -cp $jar "
			+ ExtractSpotsCLI.class
			+ " -input wikipedia-json-dump -output spot-file";

	public static void main(String[] args) {
		ExtractSpotsCLI cli = new ExtractSpotsCLI(args);
		cli.openOutput();

		SpotManager spotManager = SpotManager.getStandardSpotManager();
		IdHelper hp = IdHelperFactory.getStdIdHelper();
		RecordReader<Article> reader = new RecordReader<Article>(
				cli.getInput(), new JsonRecordParser<Article>(Article.class))
				.filter(TypeFilter.STD_FILTER);

		ProgressLogger progress = new ProgressLogger(
				"extract spots for entity {}");

		for (Article a : reader) {
			progress.up();
			int target = 0;
			int source = a.getWikiId();
			if (a.isRedirect()) {
				target = hp.getId(a.getRedirectNoAnchor());
				for (String spot : spotManager.getAllSpots(a)) {
					if (target == 0) {
						logger.warn("cannot find id for redirect label {}",
								a.getRedirectNoAnchor());
						continue;
					}
					if (target > 0) {
						// if target > 0, then target is not a disambiguation
						// (disambiguations has id < 0)
						cli.writeLineInOutput(spot + "\t" + target + "\t"
								+ target);
					}
				}
			} else {
				if (!a.isDisambiguation()) {
					for (String spot : spotManager.process(a.getTitle())) {
						cli.writeLineInOutput(spot + "\t" + source + "\t"
								+ source);
					}
				}

				for (Link l : a.getLinks()) {
					for (String spot : spotManager.process(l.getDescription())) {
						target = hp.getId(l.getCleanId());
						if (target == 0) {
							logger.warn("cannot find id for label {}",
									l.getCleanId());
							continue;
						}
						if (hp.isDisambiguation(target)) {
							logger.debug(
									"{} {} is a disambiguation (ignoring)",
									target, hp.getLabel(target));
							logger.debug("(source = {})", hp.getLabel(source));
							continue;
						}

						cli.writeLineInOutput(spot + "\t" + source + "\t"
								+ target);
					}
				}
			}
		}
		cli.closeOutput();
	}

	public ExtractSpotsCLI(String[] args) {
		super(args, params, USAGE);
	}
}
