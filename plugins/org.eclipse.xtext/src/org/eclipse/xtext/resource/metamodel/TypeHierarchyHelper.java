/*******************************************************************************
 * Copyright (c) 2008 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.xtext.resource.metamodel;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.EcoreUtil2.FindResult;
import org.eclipse.xtext.resource.metamodel.EClassifierInfo.EClassInfo;

/**
 * @author Heiko Behrens - Initial contribution and API
 * 
 */
public class TypeHierarchyHelper {
	private EClassifierInfos infos;
	private Map<EClassInfo, Set<EClassInfo>> subTypesMap = new HashMap<EClassInfo, Set<EClassInfo>>();
	private Set<EClassInfo> rootInfos = new HashSet<EClassInfo>();
	private Set<EClassInfo> traversedTypes = new HashSet<EClassInfo>();

	public TypeHierarchyHelper(EClassifierInfos infos) {
		super();
		this.infos = infos;
		collectTypeData();
	}

	private void registerSubType(EClassInfo superType, EClassInfo subType) {
		Set<EClassInfo> subTypes = getSubTypesOf(superType);
		subTypes.add(subType);
	}

	private void collectTypeData() {
		for (EClassInfo classInfo : infos.getAllEClassInfos()) {
			if (classInfo.getEClass().getESuperTypes().isEmpty())
				rootInfos.add(classInfo);
			for (EClassInfo superInfo : infos.getSuperTypeInfos(classInfo))
				registerSubType(superInfo, classInfo);
		}
	}

	public Set<EClassInfo> getSubTypesOf(EClassInfo info) {
		Set<EClassInfo> result = subTypesMap.get(info);
		if (result == null) {
			result = new HashSet<EClassInfo>();
			subTypesMap.put(info, result);
		}
		return result;
	}

	public void liftUpFeaturesRecursively(Collection<EClassInfo> infos) {
		traversedTypes.clear();
		for (EClassInfo info : infos)
			liftUpFeaturesInto(info);
	}

	public void liftUpFeaturesInto(EClassInfo superType) {
		// do not look at types twice (might happen due to multiple inheritance)
		if (traversedTypes.contains(superType))
			return;
		traversedTypes.add(superType);

		Collection<EClassInfo> subTypes = getSubTypesOf(superType);
		if (subTypes.isEmpty())
			return;

		// lift up features recursively, deepest first
		for (EClassInfo subType : subTypes) {
			liftUpFeaturesInto(subType);
		}

		// do not modify sealed types
		if (!superType.isGenerated())
			return;

		// only if all subtypes' compatible type is superType itself
		// features can be lifted into superType
		if (infos.getCompatibleTypeOf(subTypes).equals(superType)) {
			Collection<EStructuralFeature> commonFeatures = getCommonDirectFeatures(subTypes);
			Collection<EStructuralFeature> liftedFeatures = joinFeaturesInto(commonFeatures, superType);
			for (EClassInfo subClassInfo : subTypes)
				removeFeatures(subClassInfo, liftedFeatures);
		}
	}

	private void removeFeatures(EClassInfo info, Collection<EStructuralFeature> features) {
		Collection<EStructuralFeature> featuresToBeModified = info.getEClass().getEStructuralFeatures();
		for (Iterator<EStructuralFeature> iterator = featuresToBeModified.iterator(); iterator.hasNext();)
			if (EcoreUtil2.containsSemanticallyEqualFeature(features, iterator.next()) == FindResult.FeatureExists)
				iterator.remove();
		
	}

	private Collection<EStructuralFeature> joinFeaturesInto(Collection<EStructuralFeature> commonFeatures,
			EClassInfo info) {
		Collection<EStructuralFeature> result = new HashSet<EStructuralFeature>();
		for (EStructuralFeature feature : commonFeatures) {
			switch (EcoreUtil2.containsSemanticallyEqualFeature(info.getEClass(), feature)) {
				case FeatureDoesNotExist:
					info.addFeature(feature);
				case FeatureExists:
					result.add(feature);
				default:
					break;
			}
		}
		return result;
	}

	private Collection<EStructuralFeature> getCommonDirectFeatures(Collection<EClassInfo> infos) {
		Collection<EStructuralFeature> result = new HashSet<EStructuralFeature>();

		Iterator<EClassInfo> iterator = infos.iterator();
		if (iterator.hasNext()) {
			EClass eClass = iterator.next().getEClass();
			result.addAll(eClass.getEStructuralFeatures());
		}

		while (iterator.hasNext())
			result = getCommonFeatures(iterator.next(), result);

		return result;
	}

	public Collection<EStructuralFeature> getCommonFeatures(EClassInfo info, Collection<EStructuralFeature> features) {
		Collection<EStructuralFeature> result = new HashSet<EStructuralFeature>();

		for (EStructuralFeature f : features)
			if (EcoreUtil2.containsSemanticallyEqualFeature(info.getEClass(), f) == FindResult.FeatureExists)
				result.add(f);

		return result;
	}

	public void liftUpFeaturesRecursively() {
		traversedTypes.clear();
		liftUpFeaturesRecursively(rootInfos);
	}

	public void removeDuplicateDerivedFeatures() {
		removeDuplicateDerivedFeaturesOf(infos.getAllEClassInfos());
	}

	private void removeDuplicateDerivedFeaturesOf(Collection<EClassInfo> classInfos) {
		for (EClassInfo classInfo : classInfos) {
			removeDuplicateDerivedFeaturesOf(classInfo);
		}
	}

	private void removeDuplicateDerivedFeaturesOf(EClassInfo classInfo) {
		// do not modify sealed types
		if (!classInfo.isGenerated())
			return;

		Collection<EStructuralFeature> features = classInfo.getEClass().getEStructuralFeatures();
		for (Iterator<EStructuralFeature> iterator = features.iterator(); iterator.hasNext();)
			if(anySuperTypeContainsSemanticallyEqualFeature(classInfo.getEClass(), iterator.next()))
				iterator.remove();
	}

	private boolean anySuperTypeContainsSemanticallyEqualFeature(EClass eClass, EStructuralFeature feature) {
		Collection<EStructuralFeature> allSupertypesFeatures = new HashSet<EStructuralFeature>();
		for (EClass superType : eClass.getEAllSuperTypes())
			allSupertypesFeatures.addAll(superType.getEAllStructuralFeatures());

		return EcoreUtil2.containsSemanticallyEqualFeature(allSupertypesFeatures, feature) == FindResult.FeatureExists;
	}

}
