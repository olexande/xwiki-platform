/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xpn.xwiki.wysiwyg.client.widget.wizard.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.util.ResourceName;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.WizardStep;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.NavigationListener.NavigationDirection;

/**
 * Wizard step used to aggregate a set of selectors for a file attached (file attachment or image) to a page in the
 * wiki, and switch between the current page view and the entire wiki view.
 * 
 * @param <T> the type of object edited by this wizard step
 * @see AbstractSelectorWizardStep
 * @version $Id$
 */
public abstract class AbstractSelectorAggregatorWizardStep<T> extends AbstractSelectorWizardStep<T> implements
    SelectionHandler<Integer>
{
    /**
     * Loading class for the time to load the step to which it has been toggled.
     */
    private static final String STYLE_LOADING = "loading";

    /**
     * Loading class for the time to load the step to which it has been toggled.
     */
    private static final String STYLE_ERROR = "errormessage";

    /**
     * The map of wizard step instances of the steps aggregated by this step.
     */
    private Map<String, WizardStep> steps = new HashMap<String, WizardStep>();

    /**
     * The state of the initialization of the aggregated wizard steps.
     */
    private Map<WizardStep, Boolean> initialized = new HashMap<WizardStep, Boolean>();

    /**
     * The tabbed panel of the wizard step.
     */
    private final TabPanel tabPanel = new TabPanel();

    /**
     * The main panel of this wizard step.
     */
    private final FlowPanel mainPanel = new FlowPanel();

    /**
     * The current resource edited by this wizard step.
     */
    private ResourceName editedResource;

    /**
     * Creates a new aggregator selector wizard step, for the currently edited resource.
     * 
     * @param editedResource the currently edited resource
     */
    public AbstractSelectorAggregatorWizardStep(ResourceName editedResource)
    {
        this.editedResource = editedResource;

        // instantiate the main panel
        mainPanel.addStyleName("xSelectorStep");

        tabPanel.addStyleName("xStepsTabs");

        // add an empty flow panel for each step to show in the tabs panel
        for (String stepName : getStepNames()) {
            tabPanel.add(new FlowPanel(), stepName);
        }

        tabPanel.addSelectionHandler(this);
        mainPanel.add(tabPanel);
    }

    /**
     * @param name the name of the step to get
     * @return the step for the passed name
     */
    protected WizardStep getStep(String name)
    {
        if (steps.get(name) == null) {
            // save it in the steps
            steps.put(name, getStepInstance(name));
            // as uninitialized
            initialized.put(steps.get(name), false);
        }
        return steps.get(name);
    }

    /**
     * @param name the name of the step to initialize
     * @return an instance of the step recognized by the passed name
     */
    protected abstract WizardStep getStepInstance(String name);

    /**
     * @return the list of all step names
     */
    protected abstract List<String> getStepNames();

    /**
     * @return the step which should be selected by default, the first step name by default
     */
    protected String getDefaultStepName()
    {
        return getStepNames().get(0);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SelectionHandler#onSelection(SelectionEvent)
     */
    public void onSelection(SelectionEvent<Integer> event)
    {
        if (event.getSource() != tabPanel) {
            return;
        }
        tabPanel.addStyleName(STYLE_LOADING);

        // get the step to be prepared and shown
        String stepName = tabPanel.getTabBar().getTabHTML(event.getSelectedItem());
        final WizardStep stepToShow = getStep(stepName);

        final FlowPanel stepPanel = (FlowPanel) tabPanel.getWidget(tabPanel.getDeckPanel().getVisibleWidget());
        // hide its contents until after load
        if (stepPanel.getWidgetCount() > 0) {
            stepPanel.getWidget(0).setVisible(false);
        }

        // initialize only if it wasn't initialized before
        lazyInitializeStep(stepToShow, new AsyncCallback<Object>()
        {
            public void onSuccess(Object result)
            {
                // add the UI of the step we switched to to the tabbed panel, if not already there
                if (stepPanel.getWidgetCount() == 0) {
                    stepPanel.add(stepToShow.display());
                }
                // show content back
                stepPanel.getWidget(0).setVisible(true);
                tabPanel.removeStyleName(STYLE_LOADING);
            }

            public void onFailure(Throwable caught)
            {
                stepPanel.setVisible(true);
                tabPanel.removeStyleName(STYLE_LOADING);
                showError(Strings.INSTANCE.linkErrorLoadingData());
            }
        });
    }

    /**
     * Helper function to show an error in the main panel.
     * 
     * @param message the error message
     */
    private void showError(String message)
    {
        Label error = new Label(message);
        error.addStyleName(STYLE_ERROR);
        tabPanel.add(error);
    }

    /**
     * Selects the tab indicated by the passed name.
     * 
     * @param tabName the name of the tab to select
     */
    protected void selectTab(String tabName)
    {
        // searched for the specified tab and select it
        for (int i = 0; i < tabPanel.getTabBar().getTabCount(); i++) {
            if (tabPanel.getTabBar().getTabHTML(i).equals(tabName)) {
                tabPanel.selectTab(i);
                break;
            }
        }
    }

    /**
     * @return the currently selected wizard step, or the default step if no selection is made
     */
    private WizardStep getCurrentStep()
    {
        String selectedStepName = getSelectedStepName();
        return getStep(selectedStepName == null ? getDefaultStepName() : selectedStepName);
    }

    /**
     * @return the name of the currently selected wizard step, or {@code null} if no selection is made
     */
    private String getSelectedStepName()
    {
        int selectedTab = tabPanel.getTabBar().getSelectedTab();
        String currentStepName = null;
        if (selectedTab > 0) {
            currentStepName = tabPanel.getTabBar().getTabHTML(selectedTab);
        }
        return currentStepName;
    }

    /**
     * {@inheritDoc}
     */
    public Widget display()
    {
        return mainPanel;
    }

    /**
     * {@inheritDoc}
     */
    public String getDirectionName(NavigationDirection direction)
    {
        return getCurrentStep().getDirectionName(direction);
    }

    /**
     * {@inheritDoc}
     */
    public String getNextStep()
    {
        return getCurrentStep().getNextStep();
    }

    /**
     * {@inheritDoc}
     */
    public Object getResult()
    {
        return getCurrentStep().getResult();
    }

    /**
     * {@inheritDoc}
     */
    public void init(Object data, final AsyncCallback< ? > cb)
    {
        // reset initialization of aggregated steps
        for (WizardStep step : initialized.keySet()) {
            initialized.put(step, false);
        }

        super.init(data, new AsyncCallback<Object>()
        {
            public void onSuccess(Object result)
            {
                // dispatch the initialization
                dispatchInit(cb);
            }

            public void onFailure(Throwable caught)
            {
                cb.onFailure(caught);
            }
        });
    }

    /**
     * Dispatches the initialization of the tabbed panel to the appropriate step, depending on the required step, the
     * initialization of this aggregator and the current selected step, if any.
     * 
     * @param cb the initialization callback
     */
    private void dispatchInit(final AsyncCallback< ? > cb)
    {
        String stepName = getRequiredStep();
        if (stepName != null) {
            // if a requirement on the needed step is made,
            if (getSelectedStepName() == null || !getSelectedStepName().equals(stepName)) {
                // the tabs should be switched if the required step is not already selected
                selectTab(stepName);
                cb.onSuccess(null);
            } else {
                lazyInitializeStep(getCurrentStep(), cb);
            }
        } else {
            // if a requirement is not made
            if (getSelectedStepName() == null) {
                // and no selection already exists, just select the default step
                selectTab(getDefaultStepName());
                cb.onSuccess(null);
            } else {
                // otherwise, fake a selection on the current tab to initialize the current tab
                lazyInitializeStep(getCurrentStep(), cb);
            }
        }
    }

    /**
     * Initializes the passed step only if it wasn't initialized yet (i.e. it's the first display of this step).
     * 
     * @param step the step to initialize
     * @param cb the call back to handle asynchronous load of the step
     */
    private void lazyInitializeStep(WizardStep step, AsyncCallback< ? > cb)
    {
        if (!initialized.get(step)) {
            step.init(getData(), cb);
            initialized.put(step, true);
            return;
        }
        // nothing to do, just signal success
        cb.onSuccess(null);
    }

    /**
     * @return the name of the step required to be loaded by the current created or edited element, if any, or null
     *         otherwise (if previous selection should be preserved). To be overwritten by subclasses to detect whether
     *         the data being handled requires the "all pages" step to be loaded or not.
     */
    protected String getRequiredStep()
    {
        // by default, no requirement is made
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void onCancel(AsyncCallback<Boolean> async)
    {
        getCurrentStep().onCancel(async);
    }

    /**
     * {@inheritDoc}
     */
    public void onSubmit(AsyncCallback<Boolean> async)
    {
        getCurrentStep().onSubmit(async);
    }

    /**
     * @return the editedResource
     */
    public ResourceName getEditedResource()
    {
        return editedResource;
    }
}
