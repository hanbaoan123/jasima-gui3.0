/*
This file is part of jasima, v1.3, the Java simulator for manufacturing and logistics.
 
Copyright (c) 2015 		jasima solutions UG
Copyright (c) 2010-2015 Torsten Hildebrandt and jasima contributors

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package jasima_gui.dialogs.streamEditor;

import jasima_gui.dialogs.streamEditor.util.ConstraintValidator;

import java.util.ArrayList;
import java.util.Arrays;

public class DetailsPageIntEmpirical extends DetailsPageBase {

	public static final String TITLE = "Empirical integer distribution";
	public static final String DESCRIPTION = "Defines a list of (integer) values each occurring with a certain probablity.";
	public static final String INPUT_TYPE = "jasima.shopSim.util.modelDef.streams.IntEmpDef";

	public DetailsPageIntEmpirical() {
		super();

		final FormProperty prop1 = addIntegerListProperty("values", "values");
		final FormProperty prop2 = addDoubleListProperty("probs",
				"probabilities");

		// check for same number of entries
		addConstraint(new ConstraintValidator() {

			private int len1;
			private int len2;

			@Override
			public String getMessage() {
				return String
						.format("'%s' and '%s' have to contain the same number\nof elements; currently: %d and %d",
								prop1.labelText, prop2.labelText, len1, len2);
			}

			@Override
			public FormProperty[] dependsOn() {
				return new FormProperty[] { prop1, prop2 };
			}

			@Override
			public boolean isValid() {
				Object v1 = prop1.parseFromGui();
				len1 = v1 instanceof FormParseError ? -1 : ((int[]) v1).length;
				Object v2 = prop2.parseFromGui();
				len2 = v2 instanceof FormParseError ? -1
						: ((double[]) v2).length;

				return len1 == len2 && len1 > 0;
			}
		});

		// probabilities have to sum to 1
		addConstraint(new ConstraintValidator() {

			private double sum;

			@Override
			public String getMessage() {
				return String.format("'%s' have to sum up to 1, currently: %s",
						prop2.labelText, sum);
			}

			@Override
			public FormProperty[] dependsOn() {
				return new FormProperty[] { prop2 };
			}

			@Override
			public boolean isValid() {
				Object v2 = prop2.parseFromGui();
				sum = 0.0;

				if (!(v2 instanceof FormParseError)) {
					double[] ps = (double[]) v2;
					for (double p : ps) {
						sum += p;
					}
				}

				return Math.abs(sum - 1.0) < 1e-10;
			}
		});

		// no duplicate entries in 'values'
		addConstraint(new ConstraintValidator() {

			private ArrayList<Integer> dups;

			@Override
			public String getMessage() {
				return String
						.format("The following values occur more than once in '%s': %s",
								prop1.labelText, dups);
			}

			@Override
			public FormProperty[] dependsOn() {
				return new FormProperty[] { prop1 };
			}

			@Override
			public boolean isValid() {
				Object v1 = prop1.parseFromGui();
				dups = null;

				if (!(v1 instanceof FormParseError)) {
					dups = new ArrayList<Integer>();

					int[] is = (int[]) v1;
					Arrays.sort(is);

					int v = is[0];
					int i = 1;
					while (i < is.length) {
						// duplicate?
						if (is[i] == v) {
							dups.add(v);
							// report each duplicate only once
							while (i < is.length && is[i] == v) {
								i++;
							}
						}

						if (i < is.length) {
							v = is[i];
							i++;
						}
					}
				}

				return dups != null && dups.size() == 0;
			}
		});
	}

	@Override
	public String getInputType() {
		return INPUT_TYPE;
	}

	@Override
	protected String getDescription() {
		return DESCRIPTION;
	}

	@Override
	protected String getTitle() {
		return TITLE;
	}

}
