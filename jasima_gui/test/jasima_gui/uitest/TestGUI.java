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
package jasima_gui.uitest;

import jasima_gui.util.IOUtil;
import jasima_gui.util.Pointer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

@RunWith(SWTBotJunit4ClassRunner.class)
public class TestGUI {
	@Rule
	public TestName testName = new TestName();

	private static SWTWorkbenchBot bot;
	private static SWTBotShell eclipseShell;

	@BeforeClass
	public static void beforeClass() throws Exception {
		bot = new SWTWorkbenchBot();
		eclipseShell = bot.activeShell();
		SWTBotPreferences.TIMEOUT = 30000L;
		SWTBotPreferences.PLAYBACK_DELAY = 100L;
		bot.viewByTitle("Welcome").close();
	}

	@AfterClass
	public static void afterClass() {
		bot.sleep(1000L);
	}

	SWTBotTreeItem createJasimaProject() {
		return createJasimaProject(testName.getMethodName());
	}

	SWTBotTreeItem createJasimaProject(String name) {
		eclipseShell.activate();
		bot.menu("File").menu("New").menu("jasima Project").click();
		SWTBotShell newProjectShell = bot.shell("New Jasima Project");
		newProjectShell.activate();
		bot.textWithLabel("Project name:").setText(name);
		bot.button("Finish").click();
		bot.waitUntil(Conditions.shellCloses(newProjectShell));
		SWTBotTreeItem projectItem = bot.tree().getTreeItem(name);
		projectItem.select();
		projectItem.expand();
		return projectItem;
	}

	public SWTBotTreeItem waitForRunResults(final SWTBotTreeItem projectItem) {
		final Pointer<SWTBotTreeItem> retVal = new Pointer<>();
		bot.waitUntil(new ICondition() {
			@Override
			public boolean test() throws Exception {
				for (SWTBotTreeItem item : projectItem.getItems()) {
					if (item.getText().startsWith("runResults_")) {
						retVal.value = item;
						return true;
					}
				}
				return false;
			}

			@Override
			public void init(SWTBot bot) {
				// nothing
			}

			@Override
			public String getFailureMessage() {
				return "No run results found!";
			}
		});
		return retVal.value;
	}

	public void deleteProject(SWTBotTreeItem item) {
		item.contextMenu("Delete").click();
		SWTBotShell sh = bot.shell("Delete Resources");
		sh.activate();
		bot.button("OK").click();
		bot.waitUntil(Conditions.shellCloses(sh));
	}

	@Test
	public void testSimpleExperiment() {
		final SWTBotTreeItem projectItem = createJasimaProject();
		bot.menu("File").menu("New").menu("jasima Experiment").click();
		SWTBotShell sh = bot.shell("New Jasima experiment");
		sh.activate();
		bot.button("Finish").click();
		bot.waitUntil(Conditions.shellCloses(sh));

		eclipseShell.activate();
		projectItem.getNode("new_experiment.jasima").doubleClick();
		bot.toolbarButtonWithTooltip("Run Experiment").click();
		waitForRunResults(projectItem);

		deleteProject(projectItem);
	}

	@Test
	public void testCustomClass() throws Exception {
		final SWTBotTreeItem projectItem = createJasimaProject();
		bot.menu("File").menu("New").menu("Class").click();
		SWTBotShell newClass = bot.shell("New Java Class");
		newClass.activate();
		bot.textWithLabel("Name:").setText("TestPR");
		bot.textWithLabel("Superclass:").setText("jasima.shopSim.core.PR");
		bot.button("Finish").click();
		bot.waitUntil(Conditions.shellCloses(newClass));

		projectItem.select();
		bot.menu("File").menu("New").menu("jasima Experiment").click();
		SWTBotShell sh = bot.shell("New Jasima experiment");
		sh.activate();
		bot.button("Finish").click();
		bot.waitUntil(Conditions.shellCloses(sh));

		projectItem.getNode("new_experiment.jasima").doubleClick();
		bot.toolbarButtonWithTooltip("New", 2).click();
		bot.shell("Types compatible with jasima.shopSim.core.PR").activate();
		bot.text().setText("TestPR");
		bot.waitUntil(Conditions.tableHasRows(bot.table(), 1));
		bot.button("OK").click();
		bot.activeEditor().save();

		projectItem.getNode("new_experiment.jasima").doubleClick();
		bot.toolbarButtonWithTooltip("Run Experiment").click();
		waitForRunResults(projectItem);

		deleteProject(projectItem);
	}

	@Test
	public void testImportProject() throws Exception {
		eclipseShell.activate();
		bot.menu("File").menu("Import...").click();
		SWTBotShell sh = bot.shell("Import");
		sh.activate();
		bot.tree().getTreeItem("General").expand();
		bot.waitUntil(new ICondition() {
			@Override
			public void init(SWTBot bot) {
				// empty
			}

			@Override
			public boolean test() throws Exception {
				return !bot.tree().getTreeItem("General").getNodes().isEmpty();
			}

			@Override
			public String getFailureMessage() {
				return "tree item has no nodes";
			}
		});
		bot.tree().getTreeItem("General").getNode("Existing Projects into Workspace").doubleClick();

		File tmp = File.createTempFile("jasima_gui_", ".zip");
		try (OutputStream str = new FileOutputStream(tmp)) {
			IOUtil.copyFully(TestGUI.class.getResourceAsStream("test002.zip"), str);
		}

		bot.radio("Select archive file:").click();
		bot.comboBox(1).setText(tmp.getAbsolutePath());
		bot.comboBox(1).pressShortcut(0, '\n');
		bot.waitUntil(Conditions.widgetIsEnabled(bot.button("Finish")));
		bot.button("Finish").click();
		bot.waitUntil(Conditions.shellCloses(sh));
		eclipseShell.activate();
		SWTBotTreeItem projectItem = bot.tree().getTreeItem("test002");
		projectItem.select();
		projectItem.expand();

		tmp.delete();

		projectItem.getNode("new_experiment.jasima").doubleClick();
		// bot.toolbarButtonWithTooltip("Run Experiment").click();
		// waitForRunResults(projectItem);

		deleteProject(projectItem);
	}
}
