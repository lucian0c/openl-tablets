package org.openl.rules.webstudio.web.tableeditor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.openl.message.OpenLMessage;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

@ManagedBean
@RequestScoped
public class ShowMessageBean {

    public boolean isMessageOutdated() {
        return getMessage().isEmpty();
    }

    public List<OpenLMessage> getMessage() {
        String value = WebStudioUtils.getRequestParameter("summary");

        final int openLMessageId;
        try {
            openLMessageId = Integer.parseInt(value);
        } catch (Exception ignored) {
            return Collections.emptyList();
        }

        Collection<OpenLMessage> moduleMessages = WebStudioUtils.getWebStudio().getModel().getModuleMessages();
        List<OpenLMessage> openLMessage = moduleMessages.stream()
            .filter(m -> m.getId() == openLMessageId)
            .findFirst()
            .map(Collections::singletonList)
            .orElse(Collections.emptyList());

        return openLMessage;
    }
}
