'use strict';

/**
 * Document activity controller.
 */
angular.module('docs').controller('DocumentActivity', [
  '$scope', 
  '$stateParams', 
  'Restangular', 
  '$translate',
  function($scope, $stateParams, Restangular, $translate) {
    // Initialize activity model
    $scope.activity = {
      progress: 0,
      activity_type: 'document_review'
    };
    
    /**
     * Parse timestamp to date string
     * @param {string|number} timestamp 
     * @returns {string|null} ISO date string or null if invalid
     */
    const parseTimestamp = (timestamp) => {
      try {
        const date = new Date(typeof timestamp === 'string' 
          ? parseInt(timestamp, 10) 
          : timestamp);
        
        return !isNaN(date.getTime()) 
          ? date.toISOString().substring(0, 10) 
          : null;
      } catch (e) {
        console.error("Error parsing planned date:", e);
        return null;
      }
    };
    
    // Load existing activity for this document
    $scope.loadActivity = async () => {
      try {
        const { activities = [] } = await Restangular.one('useractivity/user')
          .get({
            entity_id: $stateParams.id,
            limit: 1
          });
        
        if (activities.length > 0) {
          const [activity] = activities;
          $scope.activity = {
            ...$scope.activity,
            id: activity.id,
            progress: activity.progress,
            activity_type: activity.activity_type,
            planned_date: activity.planned_date_timestamp 
              ? parseTimestamp(activity.planned_date_timestamp)
              : undefined
          };
        }
      } catch (error) {
        console.error("Failed to load activity:", error);
      }
    };
    
    // Save the activity
    $scope.saveActivity = async () => {
      try {
        const activity = {
          ...angular.copy($scope.activity),
          entity_id: $stateParams.id,
          planned_date_timestamp: $scope.activity.planned_date
            ? new Date($scope.activity.planned_date).getTime()
            : undefined
        };
        
        const { id } = await Restangular.one('useractivity').put(activity);
        
        $scope.$apply(() => {
          $scope.activity.id = id;
          $scope.activitySaved = true;
          
          setTimeout(() => {
            $scope.$apply(() => {
              $scope.activitySaved = false;
            });
          }, 2000);
        });
      } catch (error) {
        console.error("Failed to save activity:", error);
      }
    };
    
    // Format the progress for display
    $scope.formatProgress = (progress) => {
      const key = progress === 100 
        ? 'settings.user_activities.status.completed'
        : progress > 0 
          ? 'settings.user_activities.status.progressing'
          : 'settings.user_activities.status.nostart';
      
      return progress > 0 && progress < 100
        ? `${$translate.instant(key)} (${progress}%)`
        : $translate.instant(key);
    };
    
    // Get the progress bar class
    $scope.getProgressClass = (progress) => {
      return progress === 100 
        ? 'progress-bar-success'
        : progress > 0 
          ? 'progress-bar-warning'
          : 'progress-bar-danger';
    };
    
    // Initialize by loading existing activity
    $scope.loadActivity();
  }
]);
