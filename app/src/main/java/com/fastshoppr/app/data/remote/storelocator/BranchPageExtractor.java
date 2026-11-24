/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.storelocator;

import com.fastshoppr.app.domain.model.BranchPage;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javax.inject.Inject;

/** Reads branch identity from the retailer page the user is viewing in the picker WebView. */
public final class BranchPageExtractor {

  /**
   * Runs in the WebView. Prefer schema.org data, then retailer headings/meta tags, then the page
   * title. Never reads cookies, credentials, or coordinates.
   */
  public static final String SCRIPT =
      "(function(){"
          + "function clean(v){return v==null?'':String(v).replace(/\\s+/g,' ').trim();}"
          + "function meta(key){var e=document.querySelector('meta[property=\"'+key+'\"]')"
          + "||document.querySelector('meta[name=\"'+key+'\"]');return e?clean(e.content):'';}"
          + "function address(a){if(!a)return {};if(typeof a==='string')return {street:a};"
          + "var p=a.postalAddress||a.address||a;return {street:clean(p.streetAddress),"
          + "city:clean(p.addressLocality),state:clean(p.addressRegion),"
          + "postal:clean(p.postalCode)};}"
          + "function types(v){var t=v&&v['@type'];if(Array.isArray(t))return t.map(clean);"
          + "return t?[clean(t)]:[];}"
          + "function isStore(v){var t=types(v).join(' ').toLowerCase();"
          + "return t.indexOf('store')>=0||t.indexOf('place')>=0||t.indexOf('business')>=0;"
          + "}"
          + "function walk(v){if(!v||found)return;if(Array.isArray(v)){v.forEach(walk);return;}"
          + "if(typeof v!=='object')return;if(isStore(v)&&v.name){"
          + "var a=address(v.address);found={name:clean(v.name),street:a.street||'',"
          + "city:a.city||'',state:a.state||'',postal:a.postal||''};return;}"
          + "Object.keys(v).forEach(function(k){if(k!=='@graph')walk(v[k])});}"
          + "var found=null;"
          + "document.querySelectorAll('script[type=\"application/ld+json\"]').forEach(function(s){"
          + "if(found||!s.textContent)return;try{walk(JSON.parse(s.textContent))}catch(e){}});"
          + "var h1=document.querySelector('h1');"
          + "var name=found?found.name:clean(meta('og:title')||(h1?h1.innerText:''));"
          + "if(!name){var t=clean(document.title);name=t.replace(/\\s*[-|]\\s*(Walmart|The Home Depot|Home Depot|Lowe's|Target|Smiths).*$/i,'');}"
          + "var body=document.body?clean(document.body.innerText).substring(0,2000):'';"
          + "return JSON.stringify({name:name,addressLine:found?found.street:'',"
          + "city:found?found.city:'',state:found?found.state:'',"
          + "postalCode:found?found.postal:'',title:clean(document.title),bodyText:body});"
          + "})()";

  @Inject
  public BranchPageExtractor() {}

  public BranchPage parse(String jsValue) {
    JsonElement element = JsonParser.parseString(jsValue == null ? "null" : jsValue);
    if (!element.isJsonPrimitive()) {
      throw new IllegalArgumentException("Branch page extraction returned no data");
    }
    String json = element.getAsString();
    JsonObject object;
    try {
      object = JsonParser.parseString(json).getAsJsonObject();
    } catch (RuntimeException malformed) {
      throw new IllegalArgumentException("Branch page extraction returned invalid JSON", malformed);
    }
    String name = string(object, "name");
    if (name.isEmpty()) {
      throw new IllegalArgumentException("No branch name on this page");
    }
    return new BranchPage(
        name,
        string(object, "addressLine"),
        string(object, "city"),
        string(object, "state"),
        string(object, "postalCode"),
        string(object, "title"),
        string(object, "bodyText"));
  }

  private static String string(JsonObject object, String key) {
    JsonElement value = object.get(key);
    return value == null || value.isJsonNull() ? "" : value.getAsString().trim();
  }
}
