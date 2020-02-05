/*
 * generated by Xtext 2.20.0
 */
package org.sa.rainbow.configuration.ui.wizard
/*
Copyright 2020 Carnegie Mellon University

Permission is hereby granted, free of charge, to any person obtaining a copy of this 
software and associated documentation files (the "Software"), to deal in the Software 
without restriction, including without limitation the rights to use, copy, modify, merge,
 publish, distribute, sublicense, and/or sell copies of the Software, and to permit 
 persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all 
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE 
FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR 
OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
DEALINGS IN THE SOFTWARE.
 */
import java.nio.file.Paths
import org.eclipse.xtext.ui.wizard.template.FileTemplate
import org.eclipse.xtext.ui.wizard.template.IFileGenerator
import org.eclipse.xtext.ui.wizard.template.IFileTemplateProvider

/**
 * Create a list with all file templates to be shown in the template new file wizard.
 * 
 * Each template is able to generate one or more files.
 */
class RclFileTemplateProvider implements IFileTemplateProvider {
	override getFileTemplates() {
		#[new RainbowTarget]
	}
}

@FileTemplate(label="Rainbow Target", icon="file_template.png", description="Create a target definition with default files")
class RainbowTarget {
	
	val advanced = check("Advanced Properties:", false)
	val advancedGroup = group("Properties:")
	val deploymentType = combo("Deployment style:", #["Single Machine", "Multiple Machines", "Custom Deployment"], "The deployment type of Rainbow", advancedGroup);
	val customDeployment = text("Port Factory", "", "Specify the port factory to use", advancedGroup);
	
	val useAcmeAndStitch = check('Use Acme and Stitch', true, advancedGroup)
	
	new() {
		super()
	}
	
	override protected updateVariables() {
		
		advancedGroup.enabled = advanced.value
		deploymentType.enabled = advanced.value
		customDeployment.enabled = advanced.value && deploymentType.value=="Custom Deployment"
		useAcmeAndStitch.enabled = advanced.value
		if (!advanced.value) {
			deploymentType.value = "Single Machine"
			customDeployment.value = deploymentType.value
			useAcmeAndStitch.value = true
		}
	}
	
	override protected validate() {
		super.validate()
	}
	
	override generateFiles(IFileGenerator generator) {
		val replacements = ProjectHelper.generateReplacements(name, deploymentType.value, customDeployment.value, "",
			"src/main/java", Paths.get(Paths.get(folder).parent.toAbsolutePath.toString, "generated").toAbsolutePath.toString, folder, useAcmeAndStitch.value
		)
		val p = ProjectHelper.generatePattern(replacements)
		val tgtLoc = folder + "/" + name
		generator.generate(tgtLoc + "/rainbow.properties", ProjectHelper.readFile("templates/properties.rbw_template", replacements, p))
		generator.generate(tgtLoc + "/model/gauges.rbw", ProjectHelper.readFile("templates/gauges.rbw_default", replacements, p))
		generator.generate(tgtLoc + "/system/probes.rbw", ProjectHelper.readFile("templates/probes.rbw_default", replacements, p))
		generator.generate(tgtLoc + "/system/effectors.rbw", ProjectHelper.readFile("templates/effectors.rbw_default", replacements, p))
		if (useAcmeAndStitch.value) {
				generator.generate(tgtLoc + "/stitch/stitch.s", "// Edit to add StitchsStrategies and tactics")
				generator.generate(tgtLoc + "/model/model.acme", "// Edit to add Acme model")
				generator.generate(tgtLoc + "/stitch/utilities.rbw", ProjectHelper.readFile('templates/utilities.rbw_default', replacements, p))
		}
	}
	
}
