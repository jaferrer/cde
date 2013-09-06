/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdf.dd.structure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import pt.webdetails.cdf.dd.CdeEngine;
import pt.webdetails.cdf.dd.util.Utils;
import pt.webdetails.cpf.repository.IRepositoryAccess;

/**
 * Class to hold the descriptors for a .wcdf file 
 */
public class DashboardWcdfDescriptor
{
  private static final Log _logger = LogFactory.getLog(DashboardWcdfDescriptor.class);
  
  public enum DashboardRendererType
  {
    MOBILE("mobile"), BLUEPRINT("blueprint");

    String type;
    DashboardRendererType(String t) {
      type = t;
    }
  }
  
  private String _title = "";
  private String _author;
  private String _description = "";
  private String _style;
  private String _rendererType;
  private String _path;
  private List<String> _widgetParameters;
  private String  _widgetName;
  private boolean _isWidget;

  public DashboardWcdfDescriptor()
  {
    _widgetParameters = new ArrayList<String>();
  }

  /**
   * Generates a JSONObject representing this descriptor
   *
   * @return
   */
  public JSONObject toJSON()
  {
    JSONObject json = new JSONObject();
    json.put("title",        getTitle());
    json.put("author",       getAuthor());
    json.put("description",  getDescription());
    json.put("style",        getStyle());
    json.put("widgetName",   getWidgetName());
    json.put("widget",       isWidget());
    json.put("rendererType", getRendererType());
    
    JSONArray aWidgetParams = new JSONArray();
    for(String s : _widgetParameters)
    {
      aWidgetParams.add(s);
    }
    
    json.put("widgetParameters", aWidgetParams);
    return json;
  }

  public static DashboardWcdfDescriptor fromXml(Document wcdfDoc)
  {
    DashboardWcdfDescriptor wcdf = new DashboardWcdfDescriptor();
    
    wcdf.setTitle(Utils.getNodeText("/cdf/title", wcdfDoc, ""));
    wcdf.setDescription(Utils.getNodeText("/cdf/description", wcdfDoc, ""));
    wcdf.setWidget(Utils.getNodeText("/cdf/widget", wcdfDoc, "").equalsIgnoreCase("true"));
    wcdf.setWidgetName(Utils.getNodeText("/cdf/widgetName", wcdfDoc, ""));
    wcdf.setAuthor(Utils.getNodeText("/cdf/author", wcdfDoc, ""));
    wcdf.setStyle(Utils.getNodeText("/cdf/style", wcdfDoc, ""));
    wcdf.setRendererType(Utils.getNodeText("/cdf/rendererType", wcdfDoc, ""));
    
    String widgetParams = wcdfDoc.selectSingleNode("/cdf/widgetParameters") != null ? wcdfDoc.selectSingleNode("/cdf/widgetParameters").getText() : "";
    if(StringUtils.isNotEmpty(widgetParams))
    {
      wcdf.setWidgetParameters(widgetParams.split(","));
    }
    
    return wcdf;
  }
  
  public Document toXml()
  {
    Document doc = DocumentHelper.createDocument();
    Element cdfElem = doc.addElement("cdf");
    cdfElem.addElement("title").setText(StringUtils.defaultIfEmpty(this.getTitle(), ""));
    cdfElem.addElement("description").setText(StringUtils.defaultIfEmpty(this.getDescription(), ""));
    cdfElem.addElement("author").setText(StringUtils.defaultIfEmpty(this.getAuthor(), ""));
    cdfElem.addElement("style").setText(StringUtils.defaultIfEmpty(this.getStyle(), ""));
    cdfElem.addElement("rendererType").setText(StringUtils.defaultIfEmpty(this.getRendererType(), ""));
    cdfElem.addElement("widget").setText(this.isWidget() ? "true" : "false");
    cdfElem.addElement("widgetName").setText(StringUtils.defaultIfEmpty(this.getWidgetName(), ""));
    cdfElem.addElement("widgetParameters").setText(StringUtils.join(getWidgetParameters(), ","));
    
    return doc;
  }
  
  public void update(HashMap<String, Object> parameters)
  {
    if(parameters.containsKey("title"))
    {
      setTitle((String)parameters.get("title"));
    }
    if(parameters.containsKey("author"))
    {
      setAuthor((String)parameters.get("author"));
    }
    if(parameters.containsKey("description"))
    {
      setDescription((String)parameters.get("description"));
    }
    if(parameters.containsKey("style"))
    {
      setStyle((String)parameters.get("style"));
    }
    if(parameters.containsKey("rendererType"))
    {
      setRendererType((String)parameters.get("rendererType"));
    }
    if(parameters.containsKey("widgetName"))
    {
      setWidgetName((String)parameters.get("widgetName"));
    }
    if(parameters.containsKey("widget"))
    {
      setWidget("true".equals(parameters.get("widget")));
    }
    if(parameters.containsKey("widgetParameters"))
    {
      Object widgetParams = parameters.get("widgetParameters");
      String widgetParameters[] = null;
      if (widgetParams instanceof String[])
      {
        widgetParameters = (String[]) widgetParams;
      }
      else if(widgetParams != null)
      {
        String widgetParamName = widgetParams.toString();
        if(StringUtils.isNotEmpty(widgetParamName))
        {
          widgetParameters = new String[1];
          widgetParameters[0] = widgetParamName;
        }
        else
        {
          widgetParameters = new String[0];
        }
      }

      setWidgetParameters(widgetParameters);
    }
  }
  
  public String getTitle()
  {
    return _title;
  }

  public void setTitle(String title)
  {
    this._title = title;
  }

  public String getAuthor()
  {
    return _author;
  }

  public void setAuthor(String author)
  {
    this._author = author;
  }

  public String getDescription()
  {
    return _description;
  }

  public void setDescription(String description)
  {
    this._description = description;
  }

  public String getStyle()
  {
    return _style;
  }

  public void setStyle(String style)
  {
    this._style = style;
  }

  /**
   * @return the rendererType
   */
  public String getRendererType()
  {
    return _rendererType;
  }
  
  public DashboardRendererType getParsedRendererType()
  {
    // Until we consider it safe to assume that all dashboards have
    // their renderer type correctly identified, 
    // we'll have to default to assuming they're blueprint-style dashboards.
    return parseRendererType(this._rendererType, DashboardRendererType.BLUEPRINT);
  }
  
  /**
   * @param rendererType the rendererType to set
   */
  public void setRendererType(String rendererType)
  {
    this._rendererType = rendererType;
  }

  public void setPath(String wcdfFilePath)
  {
    this._path = wcdfFilePath;
  }

  public String getPath()
  {
    return _path;
  }

  public String getStructurePath()
  {
    return _path == null? null : _path.replace(".wcdf", ".cdfde");
  }
  
  public static String toStructurePath(String wcdfPath)
  {
    return wcdfPath == null ? wcdfPath : wcdfPath.replace(".wcdf", ".cdfde");
  }
  
  public String getWidgetName()
  {
    return _widgetName;
  }

  public void setWidgetName(String widgetName)
  {
    this._widgetName = widgetName;
  }

  public void setWidget(boolean isWidget)
  {
    this._isWidget = isWidget;
  }

  public boolean isWidget()
  {
    return _isWidget;
  }

  public void setWidgetParameters(String[] params)
  {
    if(params != null) 
    {
      this._widgetParameters = Arrays.asList(params);
    }
    else 
    {
      this._widgetParameters.clear();
    }
  }

  public String[] getWidgetParameters()
  {
    return this._widgetParameters.toArray(new String[0]);
  }
  
  public static DashboardWcdfDescriptor load(String wcdfFilePath) throws IOException
  {
    IRepositoryAccess repository = CdeEngine.getInstance().getEnvironment().getRepositoryAccess();
    if(!repository.resourceExists(wcdfFilePath))
    {
      return null;
    }
    
    Document wcdfDoc = repository.getResourceAsDocument(wcdfFilePath);
    DashboardWcdfDescriptor wcdf = DashboardWcdfDescriptor.fromXml(wcdfDoc);
    wcdf.setPath(wcdfFilePath);
    return wcdf;
  }
  
  public static DashboardRendererType parseRendererType(String rendererType, DashboardRendererType defaultValue)
  {
    if(!StringUtils.isEmpty(rendererType))
    {
      try
      {
        return DashboardRendererType.valueOf(rendererType.toUpperCase());
      }
      catch (IllegalArgumentException ex)
      {
        _logger.error("Bad renderer type: " + rendererType);
      }
    }
    
    return defaultValue;
  }
}