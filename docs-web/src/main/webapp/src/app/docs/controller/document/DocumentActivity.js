'use strict';

angular.module('docs').controller('DocumentActivity', [
  '$scope', 
  '$stateParams', 
  'Restangular', 
  '$translate',
  '$q',
  function($scope, $stateParams, Restangular, $translate, $q) {
    $scope.activity = {
      progress: 0,
      activity_type: 'document_review'
    };
    
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
    
    $scope.loadActivity = function() {
      return $q(function(resolve, reject) {
        Restangular.one('useractivity/user').get({
          entity_id: $stateParams.id,
          limit: 1
        }).then(function(response) {
          var data = response.plain ? response.plain() : response;
          if (data.activities && data.activities.length > 0) {
            var activity = data.activities[0];
            angular.extend($scope.activity, {
              id: activity.id,
              progress: activity.progress,
              activity_type: activity.activity_type,
              planned_date: activity.planned_date_timestamp 
                ? parseTimestamp(activity.planned_date_timestamp)
                : undefined
            });
          }
          resolve();
        }, reject);
      });
    };
    
    $scope.saveActivity = function() {
      var activity = angular.copy($scope.activity);
      activity.entity_id = $stateParams.id;
      
      if (activity.planned_date) {
        var plannedDate = new Date(activity.planned_date);
        if (!isNaN(plannedDate.getTime())) {
          activity.planned_date_timestamp = plannedDate.getTime();
        }
      }
      
      Restangular.one('useractivity').put(activity).then(function(response) {
        var data = response.plain ? response.plain() : response;
        $scope.activity.id = data.id;
        $scope.activitySaved = true;
        
        setTimeout(function() {
          $scope.$apply(function() {
            $scope.activitySaved = false;
          });
        }, 2000);
      });
    };
    
    $scope.formatProgress = function(progress) {
      if (progress === 100) {
        return $translate.instant('settings.user_activities.status.completed');
      } else if (progress > 0) {
        return $translate.instant('settings.user_activities.status.progressing') + ' (' + progress + '%)';
      } else {
        return $translate.instant('settings.user_activities.status.nostart');
      }
    };
    
    $scope.getProgressClass = function(progress) {
      if (progress === 100) {
        return 'progress-bar-success';
      } else if (progress > 0) {
        return 'progress-bar-warning';
      } else {
        return 'progress-bar-danger';
      }
    };
    
    $scope.loadActivity();
  }
]);
