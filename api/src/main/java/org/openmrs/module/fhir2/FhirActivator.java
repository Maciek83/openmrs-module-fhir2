/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.fhir2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import ca.uhn.fhir.rest.server.IResourceProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.GlobalProperty;
import org.openmrs.api.GlobalPropertyListener;
import org.openmrs.api.context.Context;
import org.openmrs.module.BaseModuleActivator;
import org.openmrs.module.Module;
import org.openmrs.module.ModuleException;
import org.openmrs.module.ModuleFactory;
import org.openmrs.module.fhir2.api.FhirService;
import org.openmrs.module.fhir2.api.dao.FhirDao;
import org.openmrs.module.fhir2.api.spi.ServiceClassLoader;
import org.openmrs.module.fhir2.api.translators.FhirTranslator;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

/**
 * This class contains the logic that is run every time this module is either started or shutdown
 */
@Slf4j
@Component
@SuppressWarnings("unused")
public class FhirActivator extends BaseModuleActivator implements ApplicationContextAware {
	
	private static ApplicationContext applicationContext;
	
	private AnnotationConfigApplicationContext childApplicationContext;
	
	private final Map<String, Set<Class<?>>> services = new HashMap<>();
	
	private final ModuleStartListener moduleStartListener = new ModuleStartListener();
	
	@Override
	public void started() {
		if (applicationContext == null) {
			throw new ModuleException("Cannot load FHIR2 module as the main application context is not available");
		}
		
		childApplicationContext = new AnnotationConfigApplicationContext();
		childApplicationContext.setParent(applicationContext);
		loadModules();
		
		Context.getAdministrationService().addGlobalPropertyListener(moduleStartListener);
		
		log.info("Started FHIR");
	}
	
	@Override
	public void stopped() {
		Context.getAdministrationService().removeGlobalPropertyListener(moduleStartListener);
		log.info("Shutdown FHIR");
	}
	
	public ConfigurableApplicationContext getApplicationContext() {
		if (childApplicationContext == null) {
			throw new IllegalStateException("This method cannot be called before the module is started");
		}
		
		return childApplicationContext;
	}
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		FhirActivator.applicationContext = applicationContext;
	}
	
	protected void loadModules() {
		ModuleFactory.getLoadedModules().forEach(this::loadModuleInternal);
		registerBeans();
		reloadContext();
	}
	
	protected void loadModule(Module module) {
		if (!services.containsKey(module.getName())) {
			loadModuleInternal(module);
			reloadContext();
		}
	}
	
	protected void unloadModule(String moduleName) {
		if (services.containsKey(moduleName)) {
			services.remove(moduleName);
			reloadContext();
		}
	}
	
	protected void reloadContext() {
		if (childApplicationContext != null) {
			childApplicationContext.refresh();
			registerBeans();
		}
	}
	
	protected void registerBeans() {
		if (childApplicationContext != null) {
			childApplicationContext.register(services.values().stream().reduce(new HashSet<>(), (s, v) -> {
				s.addAll(v);
				return s;
			}).toArray(new Class<?>[0]));
		}
	}
	
	private void loadModuleInternal(Module module) {
		ClassLoader cl = ModuleFactory.getModuleClassLoader(module);
		
		Set<Class<?>> moduleServices = services.computeIfAbsent(module.getName(), k -> new HashSet<>());
		Stream.of(FhirDao.class, FhirTranslator.class, FhirService.class, IResourceProvider.class)
		        .flatMap(c -> new ServiceClassLoader<>(c, cl).load().stream()).filter(c -> {
			        boolean result;
			        try {
				        result = c.getAnnotation(Component.class) != null;
			        }
			        catch (NullPointerException e) {
				        result = false;
			        }
			        
			        if (!result) {
				        log.warn("Skipping {} as it is not an annotated Spring Component", c);
			        }
			        
			        return result;
		        }).forEach(moduleServices::add);
	}
	
	private final class ModuleStartListener implements GlobalPropertyListener {
		
		@Override
		public boolean supportsPropertyName(String propertyName) {
			return propertyName != null && propertyName.endsWith(".started");
		}
		
		@Override
		public void globalPropertyChanged(GlobalProperty newValue) {
			getModuleName(newValue.getProperty()).ifPresent(module -> {
				if (Boolean.parseBoolean(newValue.getPropertyValue())) {
					loadModule(ModuleFactory.getModuleById(module));
				} else {
					unloadModule(module);
				}
			});
		}
		
		@Override
		public void globalPropertyDeleted(String propertyName) {
			getModuleName(propertyName).ifPresent(FhirActivator.this::unloadModule);
		}
		
		private Optional<String> getModuleName(String propertyName) {
			if (StringUtils.isBlank(propertyName)) {
				return Optional.empty();
			}
			
			int index = propertyName.indexOf(".");
			if (index < 0) {
				return Optional.of(propertyName);
			}
			
			return Optional.of(propertyName.substring(0, index));
		}
	}
}
