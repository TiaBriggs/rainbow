package org.sa.rainbow.configuration

import java.util.Map
import java.util.Set
import java.util.regex.Pattern
import org.acme.acme.AcmeComponentTypeDeclaration
import org.acme.acme.AcmeConnectorTypeDeclaration
import org.acme.acme.AcmeElementTypeDeclaration
import org.acme.acme.AcmeGroupTypeDeclaration
import org.acme.acme.AcmePortTypeDeclaration
import org.acme.acme.AcmeRoleTypeDeclaration
import org.acme.acme.AnyTypeRef
import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.EcoreUtil2
import org.eclipse.xtext.common.types.JvmDeclaredType
import org.eclipse.xtext.common.types.JvmPrimitiveType
import org.sa.rainbow.configuration.configModel.Assignment
import org.sa.rainbow.configuration.configModel.FormalParam
import org.sa.rainbow.configuration.configModel.RichString
import org.sa.rainbow.configuration.configModel.RichStringLiteral
import org.sa.rainbow.configuration.configModel.RichStringPart
import org.sa.rainbow.configuration.configModel.StringLiteral

class XtendUtils {
		static def formalTypeName(FormalParam fp, boolean keepSimple) {
		if (fp.type.java !== null)
			keepSimple?fp.type.java.referable.simpleName:fp.type.java.referable.qualifiedName
		else if (fp.type.acme !== null) {
			switch fp.type.acme.referable {
				AcmeComponentTypeDeclaration: return keepSimple?"IAcmeComponent":"org.acmestudio.acme.element.IAcmeComponent"
				AcmeConnectorTypeDeclaration: return keepSimple?"IAcmeConnector":"org.acmestudio.acme.element.IAcmeConnector"
				AcmePortTypeDeclaration: return keepSimple?"IAcmePort":"org.acmestudio.acme.element.IAcmePort"
				AcmeRoleTypeDeclaration: return keepSimple?"IAcmeRole":"org.acmestudio.acme.element.IAcmeRole"
				AcmeElementTypeDeclaration: return keepSimple?"IAcmeElement":"org.acmestudio.acme.element.IAcmeElement"
				AcmeGroupTypeDeclaration: return keepSimple?"IAcmeGroup":"org.acmestudio.acme.element.IAcmeGroup"
				default: return keepSimple?"IAcmeElement":"org.acmestudio.acme.element.IAcmeElement"
			}
		} 
		else if(fp.type.base !== null) fp.type.base.name()
	}
	
	def static getAcmeTypeName(AnyTypeRef ref) {
		switch ref {
			AcmeElementTypeDeclaration: ref.name
			AcmeComponentTypeDeclaration: ref.name
			AcmeConnectorTypeDeclaration: ref.name
			AcmePortTypeDeclaration: ref.name
			AcmeRoleTypeDeclaration: ref.name
			AcmeGroupTypeDeclaration: ref.name
		}
	}
	
	def static convertToString(FormalParam p) {
		
		if (p.type.acme !== null) {
			'''«p.name».getQualifiedName()'''
		}
		else if (p.type.java !== null) {
			val type = p.type.java.referable
			switch type {
				JvmPrimitiveType: 
					switch type.simpleName {
						 case "double", 
						 case "Double": '''Double.toString(«p.name»)'''
						 case "int",
						 case "Integer": '''Integer.toString(«p.name»)'''
						 case "String": p.name
						 case "boolean",
						 case "Boolean" : '''Boolean.toString(«p.name»)'''
						 case "char" : '''Character.toString(«p.name»)'''
					}
				JvmDeclaredType: '''«p.name».toString()'''
			}
		}
		else {
			throw new IllegalArgumentException ("Don't know how to convert parameter " + p.name)
		}
	}
	
	static def unpackString(StringLiteral literal, boolean strip) {
//			if (literal instanceof SimpleStringLiteral) {
//				val value=(literal as SimpleStringLiteral).value
//				return value
//			}
//			else {
		val rich = literal as RichString
		var str = new StringBuilder();
		for (expr : rich.expressions) {
			if (expr instanceof RichStringLiteral) {
				str.append((expr as RichStringLiteral).value.replaceAll("«", "\\${").replaceAll("»", "}"))
			} else if (expr instanceof RichStringPart) {
				str.append((expr as RichStringPart).referable.name)
			}
		}
		if (strip) {
			var s = str.toString()
			s = s.trim()
			if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
				s = s.substring(1, s.length - 1)
			}
			return s
		}
		return str.toString
	}
	
	static def fillNamedGroups(String regexp, Set<String> namedGroups) {
		var m = Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>").matcher(regexp);
		while (m.find()) {
			namedGroups.add(m.group(1))
		}
		m = Pattern.compile("\\(.*?\\)").matcher(regexp)
		var i = 1
		while (m.find()) {
			namedGroups.add(Integer.toString(i++))
		}
		
	}
	
	static def getComponentName(EObject a) {
		val st = new StringBuilder();
		if (a instanceof Assignment) st.append((a as Assignment).name)
		var parent = EcoreUtil2.getContainerOfType(a.eContainer, Assignment)
		while (parent !== null) {
			st.insert(0, ':')
			st.insert(0, parent.name);
			parent = EcoreUtil2.getContainerOfType(parent.eContainer, Assignment)
		}
		st.toString()
	}
	
	static var COMPOUND_NAME_STORE = newHashMap
	
	static def updateStore(Map<String, ?>  knownCompoundNames) {
		// Form all component names
		var compoundNames = COMPOUND_NAME_STORE.get(knownCompoundNames) as Set<String>
		if (compoundNames === null) {
			compoundNames = newHashSet
			for (n : knownCompoundNames.keySet()) {
				val split = n.split(":")
				var st = new StringBuilder()
				for (var i = 0; i < split.length-1; i++) {
					if (i != 0) st.append(":")
					st.append(split.get(i))
					compoundNames.add(st.toString)
				}
				compoundNames.add(n)
			}
			COMPOUND_NAME_STORE.put(knownCompoundNames, compoundNames)
		}
		compoundNames
	}
	
	static def isKeyProperty(Map<String, ?>  knownCompoundNames, Assignment a) {
		val compoundNames = updateStore(knownCompoundNames)
		// Check if name is there
		val cn = getComponentName(a)
		return compoundNames.contains(cn)
	}
	
	static def getPropertySuggestions(Map<String, ?> knownCompoundNames, EObject a) {
		val compoundNames = updateStore(knownCompoundNames)
		val names = newHashSet
		val cn = getComponentName(a)
		compoundNames.forEach[
			if (it.startsWith(cn)) {
				val split2 = it.split(":")
				if (it.endsWith(":")) {
					names.add((split2).get(split2.length-2))
					
				}
				else {
					names.add((split2).get(split2.length-1))
				}
				
			}
		]
		names
	}
	
}