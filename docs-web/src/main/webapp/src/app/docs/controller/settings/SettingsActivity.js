'use strict';

/**
 * Settings activity controller.
 */
angular.module('docs').controller('SettingsActivity', function($scope, $state, Restangular, $translate) {
  // State variables
  let loadingActivities = false;
  let activities = [];
  let total = 0;
  let offset = 0;
  const limit = 50;
  let showGantt = true;
  let filterUser = null;
  let filterType = null;
  let availableUsers = [];
  let availableTypes = [];
  let ganttData = {
    data: [],
    timeScale: {
      from: new Date(),
      to: new Date(),
      width: '1000px'
    }
  };

  // Expose state variables to $scope for template access
  Object.defineProperties($scope, {
    loadingActivities: { get: () => loadingActivities, set: (v) => loadingActivities = v },
    activities: { get: () => activities, set: (v) => activities = v },
    total: { get: () => total, set: (v) => total = v },
    offset: { get: () => offset, set: (v) => offset = v },
    limit: { get: () => limit },
    showGantt: { get: () => showGantt, set: (v) => showGantt = v },
    filterUser: { get: () => filterUser, set: (v) => filterUser = v },
    filterType: { get: () => filterType, set: (v) => filterType = v },
    availableUsers: { get: () => availableUsers, set: (v) => availableUsers = v },
    availableTypes: { get: () => availableTypes, set: (v) => availableTypes = v },
    ganttData: { get: () => ganttData, set: (v) => ganttData = v }
  });

  // Helper functions
  const formatActivityType = (type) => {
    return type.split('_')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  };

  const getTaskColor = (progress) => {
    const colors = {
      100: '#5cb85c',    // Success/completed - green
      70: '#5bc0de',     // Info/almost done - blue
      30: '#f0ad4e',     // Warning/in progress - orange
      default: '#d9534f' // Danger/not started - red
    };
    
    return progress === 100 ? colors[100] :
           progress >= 70 ? colors[70] :
           progress >= 30 ? colors[30] : 
           colors.default;
  };

  const lightenColor = (color, percent) => {
    if (!color?.startsWith('#')) return '#f8f8f8';
    
    const num = parseInt(color.slice(1), 16);
    const clamp = v => Math.min(Math.max(v, 0), 255);
    
    const r = clamp((num >> 16) + percent);
    const g = clamp(((num >> 8) & 0x00FF) + percent);
    const b = clamp((num & 0x0000FF) + percent);
    
    return `#${((r << 16) + (g << 8) + b).toString(16).padStart(6, '0')}`;
  };

  const darkenColor = (color, percent) => {
    if (!color?.startsWith('#')) return '#ddd';
    
    const num = parseInt(color.slice(1), 16);
    const clamp = v => Math.min(Math.max(v, 0), 255);
    
    const r = clamp((num >> 16) - percent);
    const g = clamp(((num >> 8) & 0x00FF) - percent);
    const b = clamp((num & 0x0000FF) - percent);
    
    return `#${((r << 16) + (g << 8) + b).toString(16).padStart(6, '0')}`;
  };

  // Controller methods
  $scope.toggleView = () => {
    $scope.showGantt = !$scope.showGantt;
    if ($scope.showGantt) {
      prepareGanttData();
    }
  };

  $scope.loadActivities = () => {
    loadingActivities = true;
    
    const params = {
      offset: offset,
      limit: limit,
      sort_column: 9,
      asc: false,
      ...(filterUser && { user_id: filterUser }),
      ...(filterType && { activity_type: filterType })
    };
    
    Restangular.one('useractivity')
      .get(params)
      .then(({ activities: loadedActivities, total: loadedTotal }) => {
        activities = loadedActivities;
        total = loadedTotal;
        loadingActivities = false;
        
        extractFilters();
        if (showGantt) prepareGanttData();
      })
      .catch(error => {
        console.error('Failed to load activities:', error);
        loadingActivities = false;
      });
  };

  const extractFilters = () => {
    const users = new Map();
    const types = new Set();
    
    activities.forEach(activity => {
      users.set(activity.user_id, activity.username);
      types.add(activity.activity_type);
    });
    
    availableUsers = Array.from(users, ([id, name]) => ({ id, name }));
    availableTypes = Array.from(types, type => ({ 
      id: type, 
      name: formatActivityType(type) 
    }));
  };

  const prepareGanttData = () => {
    const ganttRows = [];
    let minDate = new Date();
    let maxDate = new Date();
    
    minDate.setDate(minDate.getDate() - 30);
    maxDate.setDate(maxDate.getDate() + 30);
    
    const userGroups = new Map();
    
    activities.forEach(activity => {
      if (!userGroups.has(activity.username)) {
        userGroups.set(activity.username, []);
      }
      
      const startDate = activity.create_timestamp ? new Date(activity.create_timestamp) : new Date();
      let endDate;
      
      if (activity.completed_date_timestamp) {
        endDate = new Date(activity.completed_date_timestamp);
      } else if (activity.planned_date_timestamp) {
        endDate = new Date(activity.planned_date_timestamp);
      } else {
        endDate = new Date(startDate);
        endDate.setDate(endDate.getDate() + 7);
      }
      
      minDate = startDate < minDate ? startDate : minDate;
      maxDate = endDate > maxDate ? endDate : maxDate;
      
      userGroups.get(activity.username).push({
        id: activity.id,
        name: activity.entity_name || formatActivityType(activity.activity_type),
        start: startDate,
        end: endDate,
        progress: activity.progress,
        color: getTaskColor(activity.progress)
      });
    });
    
    userGroups.forEach((tasks, username) => {
      const processedTasks = tasks
        .sort((a, b) => a.start - b.start)
        .map((task, index, sortedTasks) => {
          task.verticalPosition = 0;
          
          const overlappingTasks = sortedTasks
            .slice(0, index)
            .filter(existingTask => !(task.end <= existingTask.start || task.start >= existingTask.end));
          
          if (overlappingTasks.length) {
            const usedPositions = new Set(overlappingTasks.map(t => t.verticalPosition));
            let position = 0;
            while (usedPositions.has(position)) position++;
            task.verticalPosition = position;
          }
          
          return task;
        });
      
      ganttRows.push({
        name: username,
        tasks: processedTasks
      });
    });
    
    ganttData = {
      data: ganttRows,
      timeScale: { from: minDate, to: maxDate }
    };
  };

  $scope.getTimelineDates = (startDate, endDate) => {
    const dates = [];
    const currentDate = new Date(startDate);
    const end = new Date(endDate);
    
    const totalDays = Math.round((end - currentDate) / (1000 * 60 * 60 * 24));
    const step = Math.max(1, Math.round(totalDays / 15));
    
    while (currentDate <= end) {
      dates.push(new Date(currentDate));
      currentDate.setDate(currentDate.getDate() + step);
    }
    
    return dates;
  };

  $scope.getTaskStyle = (task, timeScale) => {
    const startDate = new Date(task.start);
    const endDate = new Date(task.end);
    const timeScaleStart = new Date(timeScale.from);
    const timeScaleEnd = new Date(timeScale.to);
    
    const totalTimeMs = timeScaleEnd - timeScaleStart;
    const startOffset = Math.max(0, startDate - timeScaleStart);
    const duration = Math.min(
      endDate - startDate, 
      Math.max(endDate - timeScaleStart, 36 * 60 * 60 * 1000)
    );
    
    let left = (startOffset / totalTimeMs) * 100;
    let width = (duration / totalTimeMs) * 100;
    
    left = Math.min(Math.max(left, 0.5), 100);
    width = Math.min(width, 99 - left);
    
    const topPosition = 10 + (task.verticalPosition || 0) * 35;
    const backgroundColor = lightenColor(task.color, 30);
    const borderColor = darkenColor(task.color, 10);
    
    return {
      left: `${left}%`,
      width: `${width}%`,
      top: `${topPosition}px`,
      backgroundColor,
      borderColor
    };
  };
  
  $scope.applyFilters = () => {
    offset = 0;
    $scope.loadActivities();
  };

  $scope.resetFilters = () => {
    filterUser = null;
    filterType = null;
    offset = 0;
    $scope.loadActivities();
  };

  $scope.loadMore = () => {
    offset += limit;
    $scope.loadActivities();
  };

  $scope.formatDate = (timestamp) => {
    if (!timestamp) return '';
    
    try {
      const date = new Date(isNaN(timestamp) ? timestamp : parseInt(timestamp, 10));
      return isNaN(date.getTime()) ? '' : date.toLocaleDateString();
    } catch(e) {
      console.error("Error formatting date:", e);
      return '';
    }
  };

  $scope.formatProgress = (progress) => {
    if (progress === 100) {
      return $translate.instant('settings.user_activities.status.completed');
    } else if (progress > 0) {
      return `${$translate.instant('settings.user_activities.status.progressing')} (${progress}%)`;
    }
    return $translate.instant('settings.user_activities.status.nostart');
  };
  
  $scope.formatActivityType = formatActivityType;

  $scope.getProgressClass = (progress) => {
    return progress === 100 ? 'progress-bar-success' :
           progress > 0 ? 'progress-bar-warning' :
           'progress-bar-danger';
  };

  $scope.deleteActivity = (activity) => {
    if (confirm($translate.instant('settings.user_activities.confirm_delete'))) {
      Restangular.one('useractivity', activity.id).remove().then(() => {
        activities = activities.filter(a => a.id !== activity.id);
        total--;
        
        if (showGantt) prepareGanttData();
      });
    }
  };

  // Initialize
  $scope.loadActivities();
});
