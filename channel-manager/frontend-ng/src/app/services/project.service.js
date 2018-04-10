/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

class ProjectService {
  constructor(
    $http,
    $q,
    ConfigService,
    FeedbackService,
    HippoGlobal,
  ) {
    'ngInject';

    this.$http = $http;
    this.$q = $q;

    this.ConfigService = ConfigService;
    this.FeedbackService = FeedbackService;
    this.HippoGlobal = HippoGlobal;
  }

  load(mountId, projectId) {
    this.channels = [];
    this.updateListeners = [];
    this.selectListeners = [];
    this.mountId = mountId;
    this.projectId = projectId;

    this._setupProjectSync();

    return this._setupProjects();
  }

  getBaseChannelId(channelId) {
    const channel = this.channels.find(ch => ch.id === channelId);
    return channel && channel.branchOf ? channel.branchOf : channelId;
  }

  getActiveProject() {
    const url = `${this.ConfigService.getCmsContextPath()}ws/projects/activeProject`;
    return this.$http
      .get(url)
      .then(result => result.data);
  }

  updateSelectedProject(projectId) {
    const selectedProject = this.projects.find(project => project.id === projectId);
    const selectionPromise = selectedProject ? this._selectProject(projectId) : this._selectCore();

    return selectionPromise.then(() => {
      this._callListeners(this.selectListeners, projectId);
    });
  }

  registerSelectListener(cb) {
    this.selectListeners.push(cb);
  }

  registerUpdateListener(cb) {
    this.updateListeners.push(cb);
  }

  associateToProject(documentId) {
    const url = `${this.ConfigService.getCmsContextPath()}ws/projects/${this.selectedProject.id}/associate/${documentId}`;
    return this.$http
      .post(url)
      .then(() => {
        this._getAllProjects();
        this.FeedbackService.showNotification('DOCUMENT_ADDED_TO_PROJECT', {
          name: this.selectedProject.name,
        });
      });
  }

  showAddToProjectForDocument(documentId) {
    const associatedProject = this._getProjectByDocumentId(documentId);
    return this.selectedProject && !associatedProject;
  }

  _getProjectByDocumentId(documentId) {
    // returns undefined if document is not part of any project
    // and therefore is part of core
    return this.allProjects.find(project => project.documents.find(document => document.id === documentId));
  }

  _callListeners(listeners, ...args) {
    listeners.forEach(listener => listener(...args));
  }

  _setupProjects() {
    return this.$q
      .all([
        this._getProjects(),
        this._getAllProjects(),
        this._getChannels(),
      ])
      .then(() => {
        const selectedProject = this.allProjects.find(project => project.id === this.projectId);
        this.selectedProject = selectedProject;
        return this.selectedProject ? this._selectProject(this.selectedProject.id) : this._selectCore();
      });
  }

  isContentOverlayEnabled() {
    // For now, to prevent all kinds of corner cases, we only enable the content overlay
    // for unapproved projects in review so that you cannot edit documents at all.
    return !this.selectedProject || this.selectedProject.state === 'UNAPPROVED';
  }

  isComponentsOverlayEnabled() {
    return !this.selectedProject
      || this.selectedProject.state === 'UNAPPROVED'
      || (this.selectedProject.state === 'IN_REVIEW' && this._isActionEnabled('resetChannel'));
    // The action resetChannel puts a channel back into review.
    // It is only enabled if a channel has been rejected and the project is in review.
  }

  isRejectEnabled() {
    return this.selectedProject
      && this.selectedProject.state === 'IN_REVIEW'
      && this._isActionEnabled('rejectChannel');
  }

  isAcceptEnabled() {
    return this.selectedProject
      && this.selectedProject.state === 'IN_REVIEW'
      && this._isActionEnabled('approveChannel');
  }

  accept(channelId) {
    const url = `${this.ConfigService.getCmsContextPath()}ws/projects/${this.selectedProject.id}/channel/approve`;

    return this.$http
      .post(url, channelId)
      .then((response) => { this.selectedProject = response.data; })
      .catch(() => {
        this.FeedbackService.showError('PROJECT_OUT_OF_SYNC', {});
        this._callListeners(this.updateListeners);
      });
  }

  reject(channelId, message) {
    const request = {
      method: 'POST',
      url: `${this.ConfigService.getCmsContextPath()}ws/projects/${this.selectedProject.id}/channel/reject/${channelId}`,
      headers: {
        'Content-Type': 'text/plain',
      },
      data: message,
    };

    return this.$http(request)
      .then((response) => {
        this.selectedProject = response.data;
      })
      .catch(() => {
        this.FeedbackService.showError('PROJECT_OUT_OF_SYNC', {});
        this._callListeners(this.updateListeners);
      });
  }

  _isActionEnabled(action) {
    const channelInfo = this.selectedProject.channels.find(c => c.mountId === this.mountId);
    return channelInfo && channelInfo.actions[action].enabled;
  }

  _getProjects() {
    const url = `${this.ConfigService.getCmsContextPath()}ws/projects/${this.mountId}/associated-with-channel`;

    return this.$http
      .get(url)
      .then(result => result.data)
      .then((projects) => {
        this.projects = projects;
      });
  }

  _getAllProjects() {
    const url = `${this.ConfigService.getCmsContextPath()}ws/projects`;

    return this.$http
      .get(url)
      .then(result => result.data)
      .then((projects) => {
        this.allProjects = projects;
      });
  }

  _getChannels() {
    const url = `${this.ConfigService.getCmsContextPath()}ws/channels/`;

    return this.$http
      .get(url)
      .then(response => response.data)
      .then((channels) => {
        this.channels = channels;
        return channels;
      });
  }

  _setupProjectSync() {
    if (this.HippoGlobal.Projects && this.HippoGlobal.Projects.events) {
      this.projectEvents = this.HippoGlobal.Projects.events;

      this.projectEvents.unsubscribeAll();

      this.projectEvents.subscribe('project-updated', () => this._getProjects());
      this.projectEvents.subscribe('project-status-updated', () => this._callListeners(this.updateListeners));
      this.projectEvents.subscribe('project-channel-added', () => this._callListeners(this.updateListeners));
      this.projectEvents.subscribe('project-deleted', (projectId) => {
        this._getProjects().then(() => this.updateSelectedProject(projectId));
      });
      this.projectEvents.subscribe('project-channel-deleted', (projectId) => {
        this._getProjects().then(() => this.updateSelectedProject(projectId));
      });
    }
  }

  _selectProject(projectId) {
    const url = `${this.ConfigService.getCmsContextPath()}ws/projects/activeProject/${projectId}`;
    return this.$http
      .put(url);
  }

  _selectCore() {
    const url = `${this.ConfigService.getCmsContextPath()}ws/projects/activeProject`;
    return this.$http
      .delete(url);
  }
}

export default ProjectService;
