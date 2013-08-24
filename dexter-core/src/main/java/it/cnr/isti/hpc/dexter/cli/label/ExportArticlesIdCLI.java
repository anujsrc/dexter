/**
 *  Copyright 2012 Diego Ceccarelli
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
package it.cnr.isti.hpc.dexter.cli.label;

import it.cnr.isti.hpc.cli.AbstractCommandLineInterface;
import it.cnr.isti.hpc.dexter.util.TitleRedirectId;
import it.cnr.isti.hpc.io.reader.JsonRecordParser;
import it.cnr.isti.hpc.io.reader.RecordParser;
import it.cnr.isti.hpc.io.reader.RecordReader;
import it.cnr.isti.hpc.log.ProgressLogger;
import it.cnr.isti.hpc.property.ProjectProperties;
import it.cnr.isti.hpc.wikipedia.article.Article;
import it.cnr.isti.hpc.wikipedia.reader.filter.TypeFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GenerateArticleHashCLI takes the json dump of wikipedia and create a function
 * that maps each article to an int, and the reverse.
 * 
 * @author Diego Ceccarelli, diego.ceccarelli@isti.cnr.it created on 02/lug/2012
 */
public class ExportArticlesIdCLI extends AbstractCommandLineInterface {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = LoggerFactory
			.getLogger(ExportArticlesIdCLI.class);

	private static final String USAGE = "java -cp $jar "
			+ ExportArticlesIdCLI.class
			+ " -input wikipedia-dump.json -output titles.tsv";
	private static String[] params = new String[] { INPUT, OUTPUT };

	public static void main(String[] args) {
		ExportArticlesIdCLI cli = new ExportArticlesIdCLI(args);
		String input = cli.getInput();
		cli.openOutput();
	
		ProgressLogger pl = new ProgressLogger("Processed {} articles", 100000);

		RecordReader<Article> reader = new RecordReader<Article>(input,
				new JsonRecordParser<Article>(Article.class)).filter(TypeFilter.STD_FILTER);

		
		RecordParser<TitleRedirectId> encoder = new TitleRedirectId.Parser();
		for (Article a : reader) {
			pl.up();
			cli.writeLineInOutput(encoder.encode(new TitleRedirectId(a)));
		}
		cli.closeOutput();
		return;
	}

	public ExportArticlesIdCLI(String[] args) {
		super(args, params, USAGE);
	}

}