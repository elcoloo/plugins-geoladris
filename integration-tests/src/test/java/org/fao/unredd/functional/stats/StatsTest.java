package org.fao.unredd.functional.stats;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.fao.unredd.functional.AbstractIntegrationTest;
import org.fao.unredd.functional.IntegrationTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
public class StatsTest extends AbstractIntegrationTest {

	@Test
	public void testStatsService() throws Exception {
		String layerName = "bosques:provincias";
		SQLExecute(getScript("stats-service-test.sql"));
		// Get indicators must return 1 entry
		CloseableHttpResponse ret = GET("indicators", "layerId", layerName);
		assertEquals(200, ret.getStatusLine().getStatusCode());
		JSONArray indicators = (JSONArray) JSONSerializer.toJSON(IOUtils
				.toString(ret.getEntity().getContent()));
		assertEquals(1, indicators.size());

		ret = GET("indicator", "indicatorId", indicators.getJSONObject(0)
				.getString("id"), "layerId", layerName, "objectId", "1");
		assertEquals(200, ret.getStatusLine().getStatusCode());
		JSONObject root = (JSONObject) JSONSerializer.toJSON(IOUtils
				.toString(ret.getEntity().getContent()));
		assertEquals("Cobertura forestal", root.getJSONObject("title")
				.getString("text"));
		assertEquals("Evolución de la cobertura forestal por provincia", root
				.getJSONObject("subtitle").getString("text"));

		// Check xAxis
		assertEquals(1, root.getJSONArray("xAxis").size());
		JSONArray xAxisCategories = root.getJSONArray("xAxis").getJSONObject(0)
				.getJSONArray("categories");
		assertTrue(xAxisCategories.contains("1990-01-01"));
		assertTrue(xAxisCategories.contains("2000-01-01"));
		assertTrue(xAxisCategories.contains("2005-01-01"));

		// Check yAxis
		assertEquals(1, root.getJSONArray("yAxis").size());
		JSONObject yAxis = root.getJSONArray("yAxis").getJSONObject(0);
		assertEquals("Cobertura", yAxis.getJSONObject("title")
				.getString("text"));

		// Values
		assertEquals(2, root.getJSONArray("series").size());
		JSONObject serie1 = root.getJSONArray("series").getJSONObject(0);
		assertEquals("Bosque nativo", serie1.getString("name"));
		assertEquals(100, serie1.getJSONArray("data").getInt(0));
		JSONObject serie2 = root.getJSONArray("series").getJSONObject(1);
		assertEquals("Bosque cultivado", serie2.getString("name"));
		assertEquals(1000, serie2.getJSONArray("data").getInt(0));

	}

	@Test
	public void testStatsServiceTwoAxisWithTwoSeries() throws Exception {
		String layerName = "bosques:provincias";
		SQLExecute(getScript("stats-service-test-twoaxis-twoseries.sql"));
		// Get indicators must return 1 entry
		CloseableHttpResponse ret = GET("indicators", "layerId", layerName);
		assertEquals(200, ret.getStatusLine().getStatusCode());
		JSONArray indicators = (JSONArray) JSONSerializer.toJSON(IOUtils
				.toString(ret.getEntity().getContent()));
		assertEquals(1, indicators.size());

		ret = GET("indicator", "indicatorId", indicators.getJSONObject(0)
				.getString("id"), "layerId", layerName, "objectId", "1");
		assertEquals(200, ret.getStatusLine().getStatusCode());
		JSONObject root = (JSONObject) JSONSerializer.toJSON(IOUtils
				.toString(ret.getEntity().getContent()));

		// Check xAxis
		assertEquals(1, root.getJSONArray("xAxis").size());
		JSONArray xAxisCategories = root.getJSONArray("xAxis").getJSONObject(0)
				.getJSONArray("categories");
		assertTrue(xAxisCategories.contains("1990-01-01"));
		assertTrue(xAxisCategories.contains("2000-01-01"));
		assertTrue(xAxisCategories.contains("2005-01-01"));

		// Check yAxis
		assertEquals(2, root.getJSONArray("yAxis").size());
		JSONObject axis0 = root.getJSONArray("yAxis").getJSONObject(0);
		assertEquals("Número de incendios", axis0.getJSONObject("title")
				.getString("text"));
		JSONObject axis1 = root.getJSONArray("yAxis").getJSONObject(1);
		assertEquals("Superficie",
				axis1.getJSONObject("title").getString("text"));

		// Values
		assertEquals(3, root.getJSONArray("series").size());
		JSONObject serie0 = root.getJSONArray("series").getJSONObject(0);
		JSONObject serie1 = root.getJSONArray("series").getJSONObject(1);
		JSONObject serie2 = root.getJSONArray("series").getJSONObject(2);
		assertEquals("Número de incendios", serie0.getString("name"));
		assertEquals("Superficie incendiada", serie1.getString("name"));
		assertEquals("Cobertura forestal", serie2.getString("name"));
		assertEquals(0, serie0.getInt("yAxis"));
		assertEquals(1, serie1.getInt("yAxis"));
		assertEquals(1, serie2.getInt("yAxis"));
	}

	@Test
	public void testCalculateStats() throws Exception {
		String layerName = "unredd:provinces";
		SQLExecute("INSERT INTO " + testSchema
				+ ".redd_stats_metadata ("
				+ "title, "//
				+ "subtitle, "//
				+ "description, "//
				+ "y_label, "//
				+ "units, "//
				+ "tooltipsdecimals, "//
				+ "layer_name, "//
				+ "table_name_division, " //
				+ "division_field_id,"//
				+ "class_table_name, " //
				+ "class_field_name," //
				+ "date_field_name," //
				+ "table_name_data," //
				+ "graphic_type" //
				+ ")VALUES("
				+ "'Cobertura forestal',"//
				+ "'Evolución de la cobertura forestal',"//
				+ "'Muestra la evolución de la cobertura forestal a lo largo de los años',"//
				+ "'Cobertura',"//
				+ "'km²',"//
				+ "2,"//
				+ "'" + layerName + "',"//
				+ "'" + testSchema + ".stats_admin',"//
				+ "'gid',"//
				+ "'" + testSchema + ".stats_cobertura',"//
				+ "'clasificacion',"//
				+ "'fecha',"//
				+ "'" + testSchema + ".stats_results',"//
				+ "'2D'"//
				+ ")");
		Integer indicatorId = (Integer) SQLQuery("select id from " + testSchema
				+ ".redd_stats_metadata;");
		SQLExecute("SELECT redd_stats_run(" + indicatorId + ", '" + testSchema
				+ "');");

		// Check total coverage
		Float sum = (Float) SQLQuery("SELECT sum(valor) from " + testSchema
				+ ".stats_results");
		assertTrue(Math.abs(sum - 0.0015) < 0.00001);

		// Get indicators must return 1 entry
		CloseableHttpResponse ret = GET("indicators", "layerId", layerName);
		assertEquals(200, ret.getStatusLine().getStatusCode());
		JSONArray indicators = (JSONArray) JSONSerializer.toJSON(IOUtils
				.toString(ret.getEntity().getContent()));
		assertEquals(indicators.size(), 1);

		ret = GET("indicator", "indicatorId", indicators.getJSONObject(0)
				.getString("id"), "layerId", layerName, "objectId", "1");
		assertEquals(200, ret.getStatusLine().getStatusCode());
		assertTrue(ret.getEntity().getContentType().getValue()
				.contains("text/html"));
	}
}
