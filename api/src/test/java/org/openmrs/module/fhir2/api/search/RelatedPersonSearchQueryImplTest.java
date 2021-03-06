/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.fhir2.api.search;

import static org.exparity.hamcrest.date.DateMatchers.sameOrAfter;
import static org.exparity.hamcrest.date.DateMatchers.sameOrBefore;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hl7.fhir.r4.model.Person.SP_ADDRESS_CITY;
import static org.hl7.fhir.r4.model.Person.SP_ADDRESS_COUNTRY;
import static org.hl7.fhir.r4.model.Person.SP_ADDRESS_POSTALCODE;
import static org.hl7.fhir.r4.model.Person.SP_ADDRESS_STATE;
import static org.hl7.fhir.r4.model.Person.SP_BIRTHDATE;
import static org.hl7.fhir.r4.model.Person.SP_NAME;

import java.util.List;
import java.util.stream.Collectors;

import ca.uhn.fhir.rest.api.SortOrderEnum;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.RelatedPerson;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Relationship;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.TestFhirSpringConfiguration;
import org.openmrs.module.fhir2.api.dao.FhirRelatedPersonDao;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.api.translators.RelatedPersonTranslator;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = TestFhirSpringConfiguration.class, inheritLocations = false)
public class RelatedPersonSearchQueryImplTest extends BaseModuleContextSensitiveTest {
	
	private static final String RELATIONSHIP_DATA_XML = "org/openmrs/module/fhir2/api/dao/impl/FhirRelatedPersonDaoImplTest_intial_data.xml";
	
	private static final String MALE_GENDER = "male";
	
	private static final String FEMALE_GENDER = "female";
	
	private static final String OTHER_GENDER = "other";
	
	private static final String UNKNOWN_GENDER = "unknown";
	
	private static final String NULL_GENDER = null;
	
	private static final String WRONG_GENDER = "wrong-gender";
	
	private static final String NAME = "John";
	
	private static final String PARTIAL_NAME = "Joh";
	
	private static final String NOT_FOUND_NAME = "not found name";
	
	private static final String BIRTH_DATE = "1999-12-20";
	
	private static final String NOT_FOUND_BIRTH_DATE = "0001-01-01";
	
	private static final String CITY = "Johnson City";
	
	private static final String STATE = "TN";
	
	private static final String POSTAL_CODE = "37601";
	
	private static final String COUNTRY = "FakeB";
	
	private static final int START_INDEX = 0;
	
	private static final int END_INDEX = 10;
	
	@Autowired
	private FhirRelatedPersonDao dao;
	
	@Autowired
	private RelatedPersonTranslator translator;
	
	@Autowired
	private SearchQuery<Relationship, RelatedPerson, FhirRelatedPersonDao, RelatedPersonTranslator> searchQuery;
	
	@Before
	public void setup() throws Exception {
		executeDataSet(RELATIONSHIP_DATA_XML);
	}
	
	private IBundleProvider search(SearchParameterMap theParams) {
		return searchQuery.getQueryResults(theParams, dao, translator);
	}
	
	private List<IBaseResource> get(IBundleProvider results) {
		return results.getResources(START_INDEX, END_INDEX);
	}
	
	@Test
	public void shouldReturnCollectionOfRelationsForMatchOnName() {
		StringAndListParam name = new StringAndListParam().addAnd(new StringOrListParam().add(new StringParam(NAME)));
		
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.NAME_SEARCH_HANDLER, name);
		
		IBundleProvider relations = search(theParams);
		
		List<IBaseResource> relationList = get(relations);
		
		assertThat(relations, notNullValue());
		assertThat(relationList, not(empty()));
		assertThat(relationList.size(), greaterThanOrEqualTo(1));
		assertThat(((RelatedPerson) relationList.iterator().next()).getNameFirstRep().getNameAsSingleString(),
		    containsString(NAME));
	}
	
	@Test
	public void shouldReturnCollectionOfRelationsForPartialMatchOnPersonName() {
		StringAndListParam name = new StringAndListParam()
		        .addAnd(new StringOrListParam().add(new StringParam(PARTIAL_NAME)));
		
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.NAME_SEARCH_HANDLER, name);
		
		IBundleProvider relations = search(theParams);
		
		List<IBaseResource> relationList = get(relations);
		
		assertThat(relations, notNullValue());
		assertThat(relationList, not(empty()));
		assertThat(relationList.size(), greaterThanOrEqualTo(1));
		assertThat(((RelatedPerson) relationList.iterator().next()).getNameFirstRep().getNameAsSingleString(),
		    containsString(NAME));
	}
	
	@Test
	public void shouldReturnEmptyCollectionForNoMatchOnPersonName() {
		StringAndListParam name = new StringAndListParam()
		        .addAnd(new StringOrListParam().add(new StringParam(NOT_FOUND_NAME)));
		
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.NAME_SEARCH_HANDLER, name);
		
		IBundleProvider relations = search(theParams);
		
		List<IBaseResource> relationList = get(relations);
		
		assertThat(relations, notNullValue());
		assertThat(relationList, empty());
	}
	
	@Test
	public void shouldReturnCollectionOfRelationsForMatchingGenderOfRelatedPerson() {
		TokenAndListParam gender = new TokenAndListParam().addAnd(new TokenOrListParam().add(MALE_GENDER));
		
		SearchParameterMap maleGenderParams = new SearchParameterMap().addParameter(FhirConstants.GENDER_SEARCH_HANDLER,
		    gender);
		
		IBundleProvider relationships = search(maleGenderParams);
		
		List<IBaseResource> relationList = get(relationships);
		
		assertThat(relationships, notNullValue());
		assertThat(relationList, not(empty()));
		assertThat(relationList.size(), equalTo(3));
		assertThat(relationList, everyItem(hasProperty("gender", equalTo(Enumerations.AdministrativeGender.MALE))));
		
		gender = new TokenAndListParam().addAnd(new TokenOrListParam().add(FEMALE_GENDER));
		
		SearchParameterMap femaleGenderParams = new SearchParameterMap().addParameter(FhirConstants.GENDER_SEARCH_HANDLER,
		    gender);
		
		relationships = search(femaleGenderParams);
		
		relationList = get(relationships);
		
		assertThat(relationships, notNullValue());
		assertThat(relationList, not(empty()));
		assertThat(relationList.size(), equalTo(1));
		assertThat(relationList, everyItem(hasProperty("gender", equalTo(Enumerations.AdministrativeGender.FEMALE))));
		
		gender = new TokenAndListParam().addAnd(new TokenOrListParam().add(OTHER_GENDER));
		
		SearchParameterMap otherGenderParams = new SearchParameterMap().addParameter(FhirConstants.GENDER_SEARCH_HANDLER,
		    gender);
		
		relationships = search(otherGenderParams);
		
		relationList = get(relationships);
		
		assertThat(relationships, notNullValue());
		assertThat(relationList, not(empty()));
		assertThat(relationList.size(), equalTo(1));
		assertThat(relationList, everyItem(hasProperty("gender", equalTo(null))));
		
		gender = new TokenAndListParam().addAnd(new TokenOrListParam().add(NULL_GENDER));
		
		SearchParameterMap nullGenderParams = new SearchParameterMap().addParameter(FhirConstants.GENDER_SEARCH_HANDLER,
		    gender);
		
		relationships = search(nullGenderParams);
		
		relationList = get(relationships);
		
		assertThat(relationships, notNullValue());
		assertThat(relationList, not(empty()));
		assertThat(relationList.size(), equalTo(1));
		assertThat(relationList, everyItem(hasProperty("gender", equalTo(null))));
		
		gender = new TokenAndListParam().addAnd(new TokenOrListParam().add(UNKNOWN_GENDER));
		
		SearchParameterMap unknownGenderParams = new SearchParameterMap().addParameter(FhirConstants.GENDER_SEARCH_HANDLER,
		    gender);
		
		relationships = search(unknownGenderParams);
		
		relationList = get(relationships);
		
		assertThat(relationships, notNullValue());
		assertThat(relationList, not(empty()));
		assertThat(relationList.size(), equalTo(1));
		assertThat(relationList, everyItem(hasProperty("gender", equalTo(null))));
	}
	
	@Test
	public void shouldReturnEmptyCollectionForNoMatchOnGender() {
		TokenAndListParam tokenAndListParam = new TokenAndListParam().addAnd(new TokenOrListParam().add(WRONG_GENDER));
		
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.GENDER_SEARCH_HANDLER,
		    tokenAndListParam);
		
		IBundleProvider relationships = search(theParams);
		
		List<IBaseResource> relationList = get(relationships);
		
		assertThat(relationships, notNullValue());
		assertThat(relationList, is(empty()));
	}
	
	@Test
	public void shouldReturnCollectionOfRelationForMatchOnBirthDateofRelatedPerson() {
		DateRangeParam dateRangeParam = new DateRangeParam().setLowerBound(BIRTH_DATE).setUpperBound(BIRTH_DATE);
		
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.DATE_RANGE_SEARCH_HANDLER,
		    dateRangeParam);
		
		IBundleProvider relationships = search(theParams);
		
		List<IBaseResource> relationList = get(relationships);
		
		assertThat(relationships, notNullValue());
		assertThat(relationList, not(empty()));
		assertThat(relationList.size(), equalTo(1));
		assertThat(((RelatedPerson) relationList.iterator().next()).getBirthDate().toString(), startsWith(BIRTH_DATE));
	}
	
	@Test
	public void shouldReturnEmptyCollectionForNoMatchOnBirthDate() {
		DateRangeParam dateRangeParam = new DateRangeParam().setLowerBound(NOT_FOUND_BIRTH_DATE)
		        .setUpperBound(NOT_FOUND_BIRTH_DATE);
		
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.DATE_RANGE_SEARCH_HANDLER,
		    dateRangeParam);
		
		IBundleProvider relationships = search(theParams);
		
		List<IBaseResource> relationList = get(relationships);
		
		assertThat(relationships, notNullValue());
		assertThat(relationList, empty());
	}
	
	@Test
	public void shouldReturnCollectionOfRelatedPeopleForMatchOnCity() {
		StringAndListParam stringAndListParam = new StringAndListParam()
		        .addAnd(new StringOrListParam().add(new StringParam(CITY)));
		
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.ADDRESS_SEARCH_HANDLER,
		    FhirConstants.CITY_PROPERTY, stringAndListParam);
		
		IBundleProvider relationships = search(theParams);
		
		List<IBaseResource> relationList = get(relationships);
		
		assertThat(relationships, notNullValue());
		assertThat(relationList, not(empty()));
		assertThat(relationList.size(), equalTo(1));
		assertThat(((RelatedPerson) relationList.iterator().next()).getAddressFirstRep().getCity(), equalTo(CITY));
	}
	
	@Test
	public void shouldReturnCollectionOfRelatedPeopleForMatchOnState() {
		StringAndListParam stringAndListParam = new StringAndListParam()
		        .addAnd(new StringOrListParam().add(new StringParam(STATE)));
		
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.ADDRESS_SEARCH_HANDLER,
		    FhirConstants.STATE_PROPERTY, stringAndListParam);
		
		IBundleProvider relationships = search(theParams);
		
		List<IBaseResource> relationList = get(relationships);
		
		assertThat(relationships, notNullValue());
		assertThat(relationList, not(empty()));
		assertThat(relationList.size(), equalTo(1));
		assertThat(((RelatedPerson) relationList.iterator().next()).getAddressFirstRep().getState(), equalTo(STATE));
	}
	
	@Test
	public void shouldReturnCollectionOfRelatedPeopleForMatchOnPostalCode() {
		StringAndListParam stringAndListParam = new StringAndListParam()
		        .addAnd(new StringOrListParam().add(new StringParam(POSTAL_CODE)));
		
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.ADDRESS_SEARCH_HANDLER,
		    FhirConstants.POSTAL_CODE_PROPERTY, stringAndListParam);
		
		IBundleProvider relationships = search(theParams);
		
		List<IBaseResource> relationList = get(relationships);
		
		assertThat(relationships, notNullValue());
		assertThat(relationList, not(empty()));
		assertThat(relationList.size(), equalTo(1));
		assertThat(((RelatedPerson) relationList.iterator().next()).getAddressFirstRep().getPostalCode(),
		    equalTo(POSTAL_CODE));
	}
	
	@Test
	public void shouldReturnCollectionOfRelatedPeopleForMatchOnCountry() {
		StringAndListParam stringAndListParam = new StringAndListParam()
		        .addAnd(new StringOrListParam().add(new StringParam(COUNTRY)));
		
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.ADDRESS_SEARCH_HANDLER,
		    FhirConstants.COUNTRY_PROPERTY, stringAndListParam);
		
		IBundleProvider relationships = search(theParams);
		
		List<IBaseResource> relationList = get(relationships);
		
		assertThat(relationships, notNullValue());
		assertThat(relationList, not(empty()));
		assertThat(relationList.size(), equalTo(1));
		assertThat(((RelatedPerson) relationList.iterator().next()).getAddressFirstRep().getCountry(), equalTo(COUNTRY));
	}
	
	@Test
	public void shouldReturnCollectionOfRelatedPeopleSortedByName() {
		SortSpec sort = new SortSpec();
		sort.setParamName(RelatedPerson.SP_NAME);
		sort.setOrder(SortOrderEnum.ASC);
		
		List<RelatedPerson> relationshipList = getRelationListForSorting(sort);
		
		for (int i = 1; i < relationshipList.size(); i++) {
			assertThat(relationshipList.get(i - 1).getNameFirstRep().getFamily(),
			    lessThanOrEqualTo(relationshipList.get(i).getNameFirstRep().getFamily()));
		}
		
		sort.setOrder(SortOrderEnum.DESC);
		
		relationshipList = getRelationListForSorting(sort);
		
		for (int i = 1; i < relationshipList.size(); i++) {
			assertThat(relationshipList.get(i - 1).getNameFirstRep().getFamily(),
			    greaterThanOrEqualTo(relationshipList.get(i).getNameFirstRep().getFamily()));
		}
	}
	
	@Test
	public void shouldReturnCollectionOfRelatedPeopleSortedByBirthDate() {
		SortSpec sort = new SortSpec();
		sort.setParamName(RelatedPerson.SP_BIRTHDATE);
		sort.setOrder(SortOrderEnum.ASC);
		
		List<RelatedPerson> relationshipList = getRelationListForSorting(sort);
		
		for (int i = 1; i < relationshipList.size(); i++) {
			assertThat(relationshipList.get(i - 1).getBirthDate(), sameOrBefore(relationshipList.get(i).getBirthDate()));
		}
		
		sort.setOrder(SortOrderEnum.DESC);
		relationshipList = getRelationListForSorting(sort);
		
		for (int i = 1; i < relationshipList.size(); i++) {
			assertThat(relationshipList.get(i - 1).getBirthDate(), sameOrAfter(relationshipList.get(i).getBirthDate()));
		}
	}
	
	@Test
	public void shouldReturnCollectionOfRelatedPeopleSortedByCity() {
		SortSpec sort = new SortSpec();
		sort.setParamName(RelatedPerson.SP_ADDRESS_CITY);
		sort.setOrder(SortOrderEnum.ASC);
		
		List<RelatedPerson> relationshipList = getRelationListForSorting(sort);
		
		for (int i = 1; i < relationshipList.size(); i++) {
			assertThat(relationshipList.get(i - 1).getAddressFirstRep().getCity(),
			    lessThanOrEqualTo(relationshipList.get(i).getAddressFirstRep().getCity()));
		}
		
		sort.setOrder(SortOrderEnum.DESC);
		
		relationshipList = getRelationListForSorting(sort);
		
		for (int i = 1; i < relationshipList.size(); i++) {
			assertThat(relationshipList.get(i - 1).getAddressFirstRep().getCity(),
			    greaterThanOrEqualTo(relationshipList.get(i).getAddressFirstRep().getCity()));
		}
	}
	
	@Test
	public void shouldReturnCollectionOfRelatedPeopleSortedByState() {
		SortSpec sort = new SortSpec();
		sort.setParamName(RelatedPerson.SP_ADDRESS_STATE);
		sort.setOrder(SortOrderEnum.ASC);
		
		List<RelatedPerson> relationshipList = getRelationListForSorting(sort);
		
		for (int i = 1; i < relationshipList.size(); i++) {
			assertThat(relationshipList.get(i - 1).getAddressFirstRep().getState(),
			    lessThanOrEqualTo(relationshipList.get(i).getAddressFirstRep().getState()));
		}
		
		sort.setOrder(SortOrderEnum.DESC);
		
		relationshipList = getRelationListForSorting(sort);
		
		for (int i = 1; i < relationshipList.size(); i++) {
			assertThat(relationshipList.get(i - 1).getAddressFirstRep().getState(),
			    greaterThanOrEqualTo(relationshipList.get(i).getAddressFirstRep().getState()));
		}
	}
	
	@Test
	public void shouldReturnCollectionOfRelatedPeopleSortedByPostalCode() {
		SortSpec sort = new SortSpec();
		sort.setParamName(RelatedPerson.SP_ADDRESS_POSTALCODE);
		sort.setOrder(SortOrderEnum.ASC);
		
		List<RelatedPerson> relationshipList = getRelationListForSorting(sort);
		
		for (int i = 1; i < relationshipList.size(); i++) {
			assertThat(relationshipList.get(i - 1).getAddressFirstRep().getPostalCode(),
			    lessThanOrEqualTo(relationshipList.get(i).getAddressFirstRep().getPostalCode()));
		}
		
		sort.setOrder(SortOrderEnum.DESC);
		
		relationshipList = getRelationListForSorting(sort);
		
		for (int i = 1; i < relationshipList.size(); i++) {
			assertThat(relationshipList.get(i - 1).getAddressFirstRep().getPostalCode(),
			    greaterThanOrEqualTo(relationshipList.get(i).getAddressFirstRep().getPostalCode()));
		}
	}
	
	@Test
	public void shouldReturnCollectionOfRelatedPeopleSortedByCountry() {
		SortSpec sort = new SortSpec();
		sort.setParamName(RelatedPerson.SP_ADDRESS_COUNTRY);
		sort.setOrder(SortOrderEnum.ASC);
		
		List<RelatedPerson> relationshipList = getRelationListForSorting(sort);
		
		for (int i = 1; i < relationshipList.size(); i++) {
			assertThat(relationshipList.get(i - 1).getAddressFirstRep().getCountry(),
			    lessThanOrEqualTo(relationshipList.get(i).getAddressFirstRep().getCountry()));
		}
		
		sort.setOrder(SortOrderEnum.DESC);
		
		relationshipList = getRelationListForSorting(sort);
		
		for (int i = 1; i < relationshipList.size(); i++) {
			assertThat(relationshipList.get(i - 1).getAddressFirstRep().getCountry(),
			    greaterThanOrEqualTo(relationshipList.get(i).getAddressFirstRep().getCountry()));
		}
	}
	
	@Test
	public void shouldHandleComplexQuery() {
		StringAndListParam nameParam = new StringAndListParam().addAnd(new StringOrListParam().add(new StringParam(NAME)));
		
		TokenAndListParam genderParam = new TokenAndListParam().addAnd(new TokenOrListParam().add(MALE_GENDER));
		DateRangeParam birthDateParam = new DateRangeParam().setLowerBound(BIRTH_DATE).setUpperBound(BIRTH_DATE);
		StringAndListParam cityParam = new StringAndListParam().addAnd(new StringOrListParam().add(new StringParam(CITY)));
		StringAndListParam stateParam = new StringAndListParam().addAnd(new StringOrListParam().add(new StringParam(STATE)));
		
		StringAndListParam postalCodeParam = new StringAndListParam()
		        .addAnd(new StringOrListParam().add(new StringParam(POSTAL_CODE)));
		
		StringAndListParam countryParam = new StringAndListParam()
		        .addAnd(new StringOrListParam().add(new StringParam(COUNTRY)));
		
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.NAME_SEARCH_HANDLER, nameParam)
		        .addParameter(FhirConstants.GENDER_SEARCH_HANDLER, genderParam)
		        .addParameter(FhirConstants.DATE_RANGE_SEARCH_HANDLER, birthDateParam)
		        .addParameter(FhirConstants.ADDRESS_SEARCH_HANDLER, FhirConstants.CITY_PROPERTY, cityParam)
		        .addParameter(FhirConstants.ADDRESS_SEARCH_HANDLER, FhirConstants.STATE_PROPERTY, stateParam)
		        .addParameter(FhirConstants.ADDRESS_SEARCH_HANDLER, FhirConstants.POSTAL_CODE_PROPERTY, postalCodeParam)
		        .addParameter(FhirConstants.ADDRESS_SEARCH_HANDLER, FhirConstants.COUNTRY_PROPERTY, countryParam);
		
		IBundleProvider relationships = search(theParams);
		
		List<IBaseResource> relationList = get(relationships);
		
		assertThat(relationships, notNullValue());
		assertThat(relationList, not(empty()));
		assertThat(relationList.size(), greaterThanOrEqualTo(1));
		assertThat(((RelatedPerson) relationList.iterator().next()).getNameFirstRep().getNameAsSingleString(),
		    containsString(NAME));
		assertThat(relationList, everyItem(hasProperty("gender", equalTo(Enumerations.AdministrativeGender.MALE))));
		assertThat(((RelatedPerson) relationList.iterator().next()).getBirthDate().toString(), startsWith(BIRTH_DATE));
		assertThat(((RelatedPerson) relationList.iterator().next()).getAddressFirstRep().getCity(), equalTo(CITY));
		assertThat(((RelatedPerson) relationList.iterator().next()).getAddressFirstRep().getState(), equalTo(STATE));
		assertThat(((RelatedPerson) relationList.iterator().next()).getAddressFirstRep().getPostalCode(),
		    equalTo(POSTAL_CODE));
		assertThat(((RelatedPerson) relationList.iterator().next()).getAddressFirstRep().getCountry(), equalTo(COUNTRY));
	}
	
	private List<RelatedPerson> getRelationListForSorting(SortSpec sort) {
		SearchParameterMap theParams = new SearchParameterMap().setSortSpec(sort);
		
		IBundleProvider relationships = search(theParams);
		
		List<IBaseResource> relationList = get(relationships);
		
		assertThat(relationships, notNullValue());
		assertThat(relationList, not(empty()));
		assertThat(relationList.size(), greaterThan(1));
		
		List<RelatedPerson> relationshipList = relationList.stream().map(p -> (RelatedPerson) p)
		        .collect(Collectors.toList());
		
		// Remove related persons with sort parameter value null, to allow comparison while asserting.
		switch (sort.getParamName()) {
			case SP_NAME:
				relationshipList.removeIf(p -> p.getNameFirstRep() == null);
				break;
			case SP_BIRTHDATE:
				relationshipList.removeIf(p -> p.getBirthDate() == null);
				break;
			case SP_ADDRESS_CITY:
				relationshipList.removeIf(p -> p.getAddressFirstRep() == null || p.getAddressFirstRep().getCity() == null);
				break;
			case SP_ADDRESS_STATE:
				relationshipList.removeIf(p -> p.getAddressFirstRep() == null || p.getAddressFirstRep().getState() == null);
				break;
			case SP_ADDRESS_POSTALCODE:
				relationshipList
				        .removeIf(p -> p.getAddressFirstRep() == null || p.getAddressFirstRep().getPostalCode() == null);
				break;
			case SP_ADDRESS_COUNTRY:
				relationshipList
				        .removeIf(p -> p.getAddressFirstRep() == null || p.getAddressFirstRep().getCountry() == null);
				break;
		}
		
		assertThat(relationshipList.size(), greaterThan(1));
		
		return relationshipList;
	}
	
}
