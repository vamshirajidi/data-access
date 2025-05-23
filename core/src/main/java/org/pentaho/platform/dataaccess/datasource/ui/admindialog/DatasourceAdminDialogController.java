/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.platform.dataaccess.datasource.ui.admindialog;

import com.google.gwt.user.client.Window;
import org.pentaho.agilebi.modeler.services.IModelerServiceAsync;
import org.pentaho.gwt.widgets.client.dialogs.MessageDialogBox;
import org.pentaho.metadata.model.Domain;
import org.pentaho.platform.dataaccess.datasource.DatasourceInfo;
import org.pentaho.platform.dataaccess.datasource.IDatasourceInfo;
import org.pentaho.platform.dataaccess.datasource.beans.LogicalModelSummary;
import org.pentaho.platform.dataaccess.datasource.ui.service.DSWUIDatasourceService;
import org.pentaho.platform.dataaccess.datasource.ui.service.IUIDatasourceAdminService;
import org.pentaho.platform.dataaccess.datasource.ui.service.JdbcDatasourceService;
import org.pentaho.platform.dataaccess.datasource.ui.service.MetadataUIDatasourceService;
import org.pentaho.platform.dataaccess.datasource.ui.service.MondrianUIDatasourceService;
import org.pentaho.platform.dataaccess.datasource.ui.service.UIDatasourceServiceManager;
import org.pentaho.platform.dataaccess.datasource.wizard.GwtDatasourceEditorEntryPoint;
import org.pentaho.platform.dataaccess.datasource.wizard.GwtDatasourceMessages;
import org.pentaho.platform.dataaccess.datasource.wizard.service.IXulAsyncDSWDatasourceService;
import org.pentaho.platform.dataaccess.datasource.wizard.service.IXulAsyncDatasourceServiceManager;
import org.pentaho.ui.database.gwt.GwtDatabaseDialog;
import org.pentaho.ui.xul.XulComponent;
import org.pentaho.ui.xul.XulException;
import org.pentaho.ui.xul.XulServiceCallback;
import org.pentaho.ui.xul.binding.Binding;
import org.pentaho.ui.xul.binding.BindingConvertor;
import org.pentaho.ui.xul.binding.BindingException;
import org.pentaho.ui.xul.binding.BindingExceptionHandler;
import org.pentaho.ui.xul.binding.BindingFactory;
import org.pentaho.ui.xul.components.XulButton;
import org.pentaho.ui.xul.components.XulLabel;
import org.pentaho.ui.xul.components.XulMenuitem;
import org.pentaho.ui.xul.containers.XulDialog;
import org.pentaho.ui.xul.containers.XulMenupopup;
import org.pentaho.ui.xul.containers.XulTree;
import org.pentaho.ui.xul.containers.XulTreeCols;
import org.pentaho.ui.xul.stereotype.Bindable;
import org.pentaho.ui.xul.util.AbstractXulDialogController;

import java.util.List;

public class DatasourceAdminDialogController extends AbstractXulDialogController<IDatasourceInfo> implements BindingExceptionHandler {

  // ~ Static fields/initializers ======================================================================================
  private static final String IMPORT_MSG_ID = "datasourceAdminDialog.IMPORT";
  private static final String PLUGIN_MSG_ID = "datasourceAdminDialog.PLUGIN";
  private static final String REMOVE_DS_MSG_ID = "removeDatasourceConfirmationDialog.message";

  // ~ Instance fields =================================================================================================
  private BindingFactory bf;

  private IXulAsyncDatasourceServiceManager datasourceServiceManager;
  private IModelerServiceAsync modelerService;
  private IXulAsyncDSWDatasourceService dswService;
  private DatasourceAdminDialogModel datasourceAdminDialogModel = new DatasourceAdminDialogModel();

  private XulDialog datasourceAdminDialog;
  private XulDialog datasourceAdminErrorDialog;
  private XulDialog removeDatasourceConfirmationDialog;
  private XulLabel datasourceAdminErrorLabel = null;

  private XulButton datasourceAddButton;
  private XulMenupopup datasourceTypeMenuPopup;
  private XulMenuitem exportDatasourceMenuItem;
  private XulMenuitem editDatasourceMenuItem;
  private XulMenuitem removeDatasourceMenuItem;

  private Binding editDatasourceButtonBinding;
  private Binding removeDatasourceButtonBinding;
  private Binding exportDatasourceButtonBinding;

  private GwtDatabaseDialog databaseDialog;
  private boolean administrator;
  XulTree datasourceTable = null;

  XulTreeCols datasourceTreeCols = null;
  UIDatasourceServiceManager manager;
  private GwtDatasourceEditorEntryPoint entryPoint;
  private DialogListener adminDatasourceListener;
  private GwtDatasourceMessages messageBundle;

  /**
   * Sets up bindings.
   */
  @Bindable
  public void init() {
    datasourceTable = (XulTree) document.getElementById( "datasourcesListTable" ); //$NON-NLS-1$
    datasourceTreeCols = (XulTreeCols) document.getElementById( "datasourcesListCols" ); //$NON-NLS-1$
    datasourceAdminDialog = (XulDialog) document.getElementById( "datasourceAdminDialog" ); //$NON-NLS-1$
    datasourceAdminErrorDialog = (XulDialog) document.getElementById( "datasourceAdminErrorDialog" ); //$NON-NLS-1$
    removeDatasourceConfirmationDialog =
      (XulDialog) document.getElementById( "removeDatasourceConfirmationDialog" ); //$NON-NLS-1$
    datasourceAdminErrorLabel = (XulLabel) document.getElementById( "datasourceAdminErrorLabel" ); //$NON-NLS-1$

    datasourceAddButton = (XulButton) document.getElementById( "datasourceAddButton" ); //$NON-NLS-1$
    datasourceTypeMenuPopup = (XulMenupopup) document.getElementById( "datasourceTypeMenuPopup" ); //$NON-NLS-1$
    exportDatasourceMenuItem = (XulMenuitem) document.getElementById( "exportDatasourceMenuItem" ); //$NON-NLS-1$
    editDatasourceMenuItem = (XulMenuitem) document.getElementById( "editDatasourceMenuItem" ); //$NON-NLS-1$
    removeDatasourceMenuItem = (XulMenuitem) document.getElementById( "removeDatasourceMenuItem" ); //$NON-NLS-1$
    bf.setBindingType( Binding.Type.ONE_WAY );
    try {

      BindingConvertor<IDatasourceInfo, Boolean> removeDatasourceButtonConvertor =
        new BindingConvertor<IDatasourceInfo, Boolean>() {
          @Override
          public Boolean sourceToTarget( final IDatasourceInfo datasourceInfo ) {
            return datasourceInfo.isRemovable();
          }

          @Override
          public IDatasourceInfo targetToSource( final Boolean value ) {
            throw new UnsupportedOperationException();
          }
        };

      //setup binding to disable edit datasource button until user selects a datasource
      BindingConvertor<IDatasourceInfo, Boolean> editDatasourceButtonConvertor =
        new BindingConvertor<IDatasourceInfo, Boolean>() {
          @Override
          public Boolean sourceToTarget( final IDatasourceInfo datasourceInfo ) {
            return datasourceInfo.isEditable();
          }

          @Override
          public IDatasourceInfo targetToSource( final Boolean value ) {
            throw new UnsupportedOperationException();
          }
        };

      BindingConvertor<IDatasourceInfo, Boolean> exportDatasourceButtonConvertor =
        new BindingConvertor<IDatasourceInfo, Boolean>() {
          @Override
          public Boolean sourceToTarget( final IDatasourceInfo datasourceInfo ) {
            return datasourceInfo.isExportable();
          }

          @Override
          public IDatasourceInfo targetToSource( final Boolean value ) {
            throw new UnsupportedOperationException();
          }
        };
      removeDatasourceButtonBinding = bf.createBinding( datasourceAdminDialogModel, "selectedDatasource", //$NON-NLS-1$
        removeDatasourceMenuItem, "!disabled", removeDatasourceButtonConvertor ); //$NON-NLS-1$
      editDatasourceButtonBinding = bf.createBinding( datasourceAdminDialogModel, "selectedDatasource", //$NON-NLS-1$
        editDatasourceMenuItem, "!disabled", editDatasourceButtonConvertor ); //$NON-NLS-1$
      exportDatasourceButtonBinding = bf.createBinding( datasourceAdminDialogModel, "selectedDatasource", //$NON-NLS-1$
        exportDatasourceMenuItem, "!disabled", exportDatasourceButtonConvertor ); //$NON-NLS-1$

      bf.createBinding( datasourceAdminDialogModel, "datasources", datasourceTable, "elements" );
      bf.createBinding( datasourceTable, "selectedItems", datasourceAdminDialogModel, "selectedDatasource",
        //$NON-NLS-1$//$NON-NLS-2$
        new BindingConvertor<List, IDatasourceInfo>() {

          @Override
          public IDatasourceInfo sourceToTarget( List datasources ) {
            if ( datasources != null && datasources.size() > 0 ) {
              return (IDatasourceInfo) datasources.get( 0 );
            }
            return null;
          }

          @Override
          public List targetToSource( IDatasourceInfo arg0 ) {
            throw new UnsupportedOperationException();
          }

        } ).fireSourceChanged();

      bf.createBinding( datasourceTable, "selectedItem", datasourceAdminDialogModel, "selectedDatasource" )
        .fireSourceChanged();

      removeDatasourceButtonBinding.fireSourceChanged();
      editDatasourceButtonBinding.fireSourceChanged();
      exportDatasourceButtonBinding.fireSourceChanged();
      setupNativeHooks( this );
      // Initialize UI Datasource Service Manager
      adminDatasourceListener = new DialogListener() {

        @Override
        public void onDialogAccept( Object returnValue ) {
          refreshDatasourceList();
        }

        @Override
        public void onDialogCancel() {
          // TODO Auto-generated method stub

        }

        @Override
        public void onDialogReady() {
          // TODO Auto-generated method stub

        }

        @Override
        public void onDialogError( String errorMessage ) {
          openErrorDialog( "Error", errorMessage );
        }
      };

      manager = UIDatasourceServiceManager.getInstance();

    } catch ( Exception e ) {
      System.out.println( e.getMessage() );
      e.printStackTrace();
    }
  }

  private native void setupNativeHooks( DatasourceAdminDialogController controller )/*-{
    $wnd.pho.showDatasourceAdminErrorDialog = function(title, errorMessage) {
      controller.@org.pentaho.platform.dataaccess.datasource.ui.admindialog.DatasourceAdminDialogController::openErrorDialog(Ljava/lang/String;Ljava/lang/String;)(title, errorMessage);
    }
    $wnd.pho.refreshDatasourceList = function() {
      controller.@org.pentaho.platform.dataaccess.datasource.ui.admindialog.DatasourceAdminDialogController::refreshDatasourceList()();
    }

  }-*/;

  /**
   * ID of this controller. This is how event handlers are referenced in <code>.xul</code> files.
   */
  @Override
  public String getName() {
    return "datasourceAdminDialogController"; //$NON-NLS-1$
  }

  public void setBindingFactory( final BindingFactory bf ) {
    this.bf = bf;
    this.bf.setExceptionHandler( this );
  }

  public void setDatasourceServiceManager( final IXulAsyncDatasourceServiceManager datasourceServiceManager ) {
    this.datasourceServiceManager = datasourceServiceManager;
  }

  public void setModelerService( final IModelerServiceAsync modelerService ) {
    this.modelerService = modelerService;
  }

  public void setDSWService( final IXulAsyncDSWDatasourceService dswService ) {
    this.dswService = dswService;
  }

  private void refreshDatasourceList() {
    if ( messageBundle != null ) {
      DatasourceInfo.setMessageBundle( messageBundle );
    }
    datasourceAdminDialogModel.setDatasourcesList( null );
    manager.getIds( new XulServiceCallback<List<IDatasourceInfo>>() {

      @Override
      public void success( List<IDatasourceInfo> infoList ) {
        // Because refreshDatasourceList() is called within the dialog and
        // show dialog rebuilds the dialog, always calling it would impede
        // the ability to restore focus to an element of the dialog
        if ( getDialog().isHidden() ) {
          DatasourceAdminDialogController.super.showDialog();
        }
        datasourceAdminDialogModel.setDatasourcesList( infoList );
        getDatasourceTypes();
        exportDatasourceMenuItem.setDisabled( true );
        editDatasourceMenuItem.setDisabled( true );
        removeDatasourceMenuItem.setDisabled( true );
      }

      @Override
      public void error( String message, Throwable error ) {
        openErrorDialog( "Error", message + error.getMessage() );
      }
    } );
  }

  private void getDatasourceTypes() {
    List<String> datasourceTypes = manager.getTypes();
    // Clear out the current component list
    List<XulComponent> components = datasourceTypeMenuPopup.getChildNodes();
    // remove exist import and plugins
    for ( int i = 0; i < components.size(); i++ ) {
      XulComponent component = components.get( i );
      if ( component.getId() == null ) {
        continue;
      }
      if ( component.getId().startsWith( "import" ) || component.getId().startsWith( "plugin" ) ) {
        // remove import and plugins items
        datasourceTypeMenuPopup.removeComponent( component );
      }
    }
    // find separator
    components = datasourceTypeMenuPopup.getChildNodes();
    int beforePlugins = 0;
    XulComponent beforePluginsMenuItem = null;
    for ( int i = 0; i < components.size(); i++ ) {
      XulComponent component = components.get( i );
      if ( "beforePlugins".equals( component.getId() ) ) {
        beforePlugins = i;
        beforePluginsMenuItem = component;
      }
    }
    List<IDatasourceInfo> datasourceInfoList = datasourceAdminDialogModel.getDatasourcesList();

    // Add "Import..." items first
    for ( String datasourceType : datasourceTypes ) {
      IUIDatasourceAdminService datasourceAdminService = manager.getService( datasourceType );
      if ( datasourceAdminService instanceof DSWUIDatasourceService ) {
        // Data Source Wizard
        continue;
      }
      if ( datasourceAdminService instanceof JdbcDatasourceService ) {
        // JDBC
        continue;
      }
      if ( !( datasourceAdminService instanceof MondrianUIDatasourceService ) //Analysis - import
        && !( datasourceAdminService instanceof MetadataUIDatasourceService ) ) { //Metadata - import
        continue;
      }

      if ( !datasourceAdminService.isCreatable() ) {
        continue;
      }

      XulMenuitem menuItem;
      try {
        String displayName = DatasourceInfo.getDisplayType( datasourceType, messageBundle );
        String label = messageBundle.getString( IMPORT_MSG_ID, displayName );
        menuItem = (XulMenuitem) document.createElement( "menuitem" );
        menuItem.setLabel( label );
        menuItem.setCommand( getName() + ".launchNewUI(\"" + datasourceType + "\")" );
        menuItem.setId( "import" + datasourceType );
        datasourceTypeMenuPopup.addChildAt( menuItem, beforePlugins++ );
      } catch ( XulException e ) {
        throw new RuntimeException( e );
      }
    }

    // Add plugin items
    boolean hasPlugins = false;
    beforePlugins++;
    for ( String datasourceType : datasourceTypes ) {
      IUIDatasourceAdminService datasourceAdminService = manager.getService( datasourceType );
      if ( datasourceAdminService instanceof DSWUIDatasourceService ) {
        // Data Source Wizard
        continue;
      }
      if ( datasourceAdminService instanceof JdbcDatasourceService ) {
        // JDBC
        continue;
      }
      if ( datasourceAdminService instanceof MondrianUIDatasourceService ) {
        // Analysis - import
        continue;
      } else if ( datasourceAdminService instanceof MetadataUIDatasourceService ) {
        // Metadata - import
        continue;
      }

      if ( !datasourceAdminService.isCreatable() ) {
        continue;
      }

      hasPlugins = true;
      XulMenuitem menuItem;
      try {
        String displayName = DatasourceInfo.getDisplayType( datasourceType, messageBundle );
        String label = messageBundle.getString( PLUGIN_MSG_ID, displayName );
        menuItem = (XulMenuitem) document.createElement( "menuitem" );
        menuItem.setLabel( label );
        menuItem.setCommand( getName() + ".launchNewUI(\"" + datasourceType + "\")" );
        menuItem.setId( "plugin" + datasourceType );
        datasourceTypeMenuPopup.addChildAt( menuItem, beforePlugins++ );
      } catch ( XulException e ) {
        throw new RuntimeException( e );
      }
    }
    beforePluginsMenuItem.setVisible( hasPlugins );
    datasourceAdminDialogModel.setDatasourceTypeList( datasourceTypes );
  }

  @Bindable
  public void launchNewUI( String datasourceType ) {
    IUIDatasourceAdminService service = manager.getService( datasourceType );
    String newUI = service.getNewUI();
    if ( newUI != null && newUI.length() > 0 ) {
      if ( newUI.indexOf( "builtin:" ) >= 0 ) {
        if ( service.getType().equals( JdbcDatasourceService.TYPE ) ) {
          entryPoint.showDatabaseDialog( adminDatasourceListener );
        } else if ( service.getType().equals( MondrianUIDatasourceService.TYPE ) ) {
          entryPoint.showAnalysisImportDialog( adminDatasourceListener );
        } else if ( service.getType().equals( MetadataUIDatasourceService.TYPE ) ) {
          entryPoint.showMetadataImportDialog( adminDatasourceListener );
        } else if ( service.getType().equals( DSWUIDatasourceService.TYPE ) ) {
          entryPoint.showWizard( true, adminDatasourceListener );
        }
      } else if ( newUI.indexOf( "javascript:" ) >= 0 ) {
        String script = newUI.substring( newUI.indexOf( ":" ) + 1 );
        executeJavaScript( script );
      }

    }
  }

  private native void executeJavaScript( String script ) /*-{
    try{
      var callback = "{ onCancel: function(){} ,onOk: function(returnValue){$wnd.pho.refreshDatasourceList();},onError: function(errorMessage){$wnd.pho.showDatasourceAdminErrorDialog('Error', errorMessage);}}";
      var updatedScript = script.replace(/\{callback\}/g, callback);
      eval(updatedScript);
    } catch (e){
      $wnd.pho.showDatasourceAdminErrorDialog("Error", e.message);
    }
  }-*/;

  @Override
  public void showDialog() {
    refreshDatasourceList();
  }

  @Override
  protected XulDialog getDialog() {
    return datasourceAdminDialog;
  }

  @Override
  protected IDatasourceInfo getDialogResult() {
    return datasourceAdminDialogModel.getDatasourcesList().get( datasourceAdminDialogModel.getSelectedIndex() );
  }

  @Bindable
  public void openErrorDialog( String title, String message ) {
    datasourceAdminErrorDialog.setTitle( title );
    datasourceAdminErrorLabel.setValue( message );
    datasourceAdminErrorDialog.show();
  }

  @Bindable
  public void closeErrorDialog() {
    if ( !datasourceAdminErrorDialog.isHidden() ) {
      datasourceAdminErrorDialog.hide();
      refreshDatasourceList();
    }
  }

  public void setEntryPoint( GwtDatasourceEditorEntryPoint entryPoint ) {
    this.entryPoint = entryPoint;
  }

  @Override
  public void handleException( BindingException t ) {
    t.printStackTrace();
  }

  @Bindable
  public void export() {
    IDatasourceInfo dsInfo = datasourceAdminDialogModel.getSelectedDatasource();
    if ( dsInfo == null ) {
      showErrorDialog( "datasourceAdminErrorDialog.SELECT_DATASOURCE",
        "datasourceAdminErrorDialog.SELECT_DATASOURCE_EXPORT" );
      return;
    }
    if ( JdbcDatasourceService.TYPE.equals( dsInfo.getType() ) ) {
      showErrorDialog( "datasourceAdminErrorDialog.CANNOT_EXPORT_HEADER",
        "datasourceAdminErrorDialog.CANNOT_EXPORT_TEXT" );
      return;
    }

    manager.exportDatasource( dsInfo );
  }

  @Bindable
  public void remove() {
    IDatasourceInfo dsInfo = datasourceAdminDialogModel.getSelectedDatasource();
    if ( dsInfo == null ) {
      showErrorDialog( "datasourceAdminErrorDialog.SELECT_DATASOURCE",
        "datasourceAdminErrorDialog.SELECT_DATASOURCE_DELETE" );
      return;
    }

    if ( messageBundle != null ) {
      XulLabel removeDatasourceConfirmationDialogLabel = (XulLabel) removeDatasourceConfirmationDialog.getElementById( "removeDatasourceConfirmationDialogLabel" );
      removeDatasourceConfirmationDialogLabel.setValue( messageBundle.getString( REMOVE_DS_MSG_ID, dsInfo.getName() ) );
    }

    removeDatasourceConfirmationDialog.show();
  }

  @Bindable
  public void edit() {
    IDatasourceInfo dsInfo = datasourceAdminDialogModel.getSelectedDatasource();
    if ( dsInfo == null ) {
      showErrorDialog( "datasourceAdminErrorDialog.SELECT_DATASOURCE", "datasourceAdminErrorDialog.SELECT_DATASOURCE_EDIT" );
      return;
    }
    String type = dsInfo.getType();
    final String dsId = dsInfo.getId();
    if ( DSWUIDatasourceService.TYPE.equals( type ) ) {
      dswService.getLogicalModels( dsId, new XulServiceCallback<List<LogicalModelSummary>>() {

        @Override
        public void success( List<LogicalModelSummary> retVal ) {
          for ( LogicalModelSummary logicalModelSummary : retVal ) {
            if ( !dsId.equals( logicalModelSummary.getDomainId() ) ) {
              continue;
            }
            entryPoint.showWizardEdit(
              logicalModelSummary.getDomainId(),
              logicalModelSummary.getModelId(),
              false,
              new DialogListener<Domain>() {

                @Override
                public void onDialogAccept(
                  Domain returnValue ) {
                  // TODO Auto-generated method stub

                }

                @Override
                public void onDialogCancel() {
                  // TODO Auto-generated method stub

                }

                @Override
                public void onDialogReady() {
                  // TODO Auto-generated method stub

                }

                @Override
                public void onDialogError( String errorMessage ) {
                  // TODO Auto-generated method stub

                }

              }
            );
          }
        }

        @Override
        public void error( String message, Throwable error ) {
          // TODO Auto-generated method stub

        }

      } );
    } else if ( JdbcDatasourceService.TYPE.equals( type ) ) {
      entryPoint.showEditDatabaseDialog(
        adminDatasourceListener,
        dsId
      );
    } else if ( MondrianUIDatasourceService.TYPE.equals( type ) ) {
      IDatasourceInfo datasourceInfo = datasourceAdminDialogModel.getSelectedDatasource();
      entryPoint.showEditAnalysisDialog( adminDatasourceListener, datasourceInfo );
    } else if ( MetadataUIDatasourceService.TYPE.equals( type ) ) {
      showErrorDialog( "datasourceAdminErrorDialog.CANNOT_EDIT_HEADER",
        "datasourceAdminErrorDialog.CANNOT_EDIT_TEXT" );
      return;
    }
  }

  @Bindable
  public void newConnection() {
    launchNewUI( "JDBC" );
  }

  @Bindable
  public void newDataSource() {
    launchNewUI( "Data Source Wizard" );
  }

  @Bindable
  public void removeDatasourceAccept() {
    final IDatasourceInfo dsInfo = datasourceAdminDialogModel.getSelectedDatasource();
    manager.remove( dsInfo, new XulServiceCallback<Boolean>() {

      @Override
      public void success( Boolean isOk ) {
        if ( isOk ) {
          refreshDatasourceList();
          editDatasourceMenuItem.setDisabled( true );
        } else {
          Window.alert(
            messageBundle.getString( "datasourceAdminDialogController.COULD_NOT_REMOVE" )
              + ": " + dsInfo.getId() );
        }
      }

      @Override
      public void error( String message, Throwable error ) {
        Window.alert(
          messageBundle.getString( "datasourceAdminDialogController.ERROR_REMOVING" )
            + ": " + dsInfo.getId() + "."
            + messageBundle.getString( "ERROR" ) + "=" + error.getLocalizedMessage() );
      }
    } );
    removeDatasourceConfirmationDialog.hide();
  }

  @Bindable
  public void removeDatasourceCancel() {
    removeDatasourceConfirmationDialog.hide();
  }

  public void setMessageBundle( GwtDatasourceMessages messageBundle ) {
    this.messageBundle = messageBundle;
  }

  private void showErrorDialog( String titleKey, String messageKey ) {
    MessageDialogBox b = new MessageDialogBox(
      messageBundle.getString( titleKey ),
      messageBundle.getString( messageKey ), false, false, true,
      messageBundle.getString( "dialog.button.ok" ) );
    b.getElement().getStyle().setZIndex( Short.MAX_VALUE );
    b.center();
    b.show();
  }
}
