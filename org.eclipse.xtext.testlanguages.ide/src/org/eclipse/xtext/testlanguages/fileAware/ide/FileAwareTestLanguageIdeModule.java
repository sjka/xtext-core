/*
 * generated by Xtext
 */
package org.eclipse.xtext.testlanguages.fileAware.ide;

import org.eclipse.xtext.ide.refactoring.IResourceRelocationStrategy;
import org.eclipse.xtext.ide.serializer.hooks.IReferenceUpdater;
import org.eclipse.xtext.testlanguages.fileAware.ide.refactoring.FileAwareTestLanguageReferenceUpdater;
import org.eclipse.xtext.testlanguages.fileAware.ide.refactoring.FileAwareTestLanguageResourceRelocationStrategy;

/**
 * Use this class to register ide components.
 */
public class FileAwareTestLanguageIdeModule extends AbstractFileAwareTestLanguageIdeModule {
	
	public Class<? extends IResourceRelocationStrategy> bindIResourceRelocationStrategy() {
		return FileAwareTestLanguageResourceRelocationStrategy.class;
	}

	public Class<? extends IReferenceUpdater> bindReferenceUpdater() {
		return FileAwareTestLanguageReferenceUpdater.class;
	}
}
