'use strict';

/**
 * Login controller.
 */
angular.module('docs').controller('Login', function(Restangular, $scope, $rootScope, $state, $stateParams, $dialog, User, $translate, $uibModal) {
  $scope.codeRequired = false;

  // Get the app configuration
  Restangular.one('app').get().then(function(data) {
    $rootScope.app = data;
  });

  // Login as guest
  $scope.loginAsGuest = function() {
    $scope.guestLoginStatus = 1;
    $rootScope.randomToken && pollGuestLoginStatus($rootScope.randomToken);
  };

  function pollGuestLoginStatus(token) {
    Restangular.one('user').post('login_request', 
      JSON.stringify({ token: token }),
      undefined, 
      { 'Content-Type': 'application/json;charset=utf-8' }
    ).then(
      ({ status, username, password }) => {
        $scope.guestLoginStatus = status;
        
        switch (status) {
          case 2:
            if (username && (password || localStorage.password)) {
              $scope.user = {
                username,
                password: password || localStorage.password
              };
              password && (localStorage.password = password);
              $scope.login();
            }
            break;
            
          case 3:
            $dialog.messageBox(
              $translate.instant('login.rejected_title'),
              $translate.instant('login.rejected_message'),
              [{ result: 'ok', label: $translate.instant('ok'), cssClass: 'btn-primary' }]
            );
            break;
            
          case 1:
            setTimeout(() => pollGuestLoginStatus(token), 2000);
            break;
        }
      },
      () => {
        $scope.guestLoginStatus = 0;
      }
    );
  }
  
  // Login
  $scope.login = function() {
    User.login($scope.user).then(function() {
      User.userInfo(true).then(function(data) {
        $rootScope.userInfo = data;
      });

      if($stateParams.redirectState !== undefined && $stateParams.redirectParams !== undefined) {
        $state.go($stateParams.redirectState, JSON.parse($stateParams.redirectParams))
          .catch(function() {
            $state.go('document.default');
          });
      } else {
        $state.go('document.default');
      }
    }, function(data) {
      if (data.data.type === 'ValidationCodeRequired') {
        // A TOTP validation code is required to login
        $scope.codeRequired = true;
      } else {
        // Login truly failed
        var title = $translate.instant('login.login_failed_title');
        var msg = $translate.instant('login.login_failed_message');
        var btns = [{result: 'ok', label: $translate.instant('ok'), cssClass: 'btn-primary'}];
        $dialog.messageBox(title, msg, btns);
      }
    });
  };

  // Password lost
  $scope.openPasswordLost = function () {
    $uibModal.open({
      templateUrl: 'partial/docs/passwordlost.html',
      controller: 'ModalPasswordLost'
    }).result.then(function (username) {
      if (username === null) {
        return;
      }

      // Send a password lost email
      Restangular.one('user').post('password_lost', {
        username: username
      }).then(function () {
        var title = $translate.instant('login.password_lost_sent_title');
        var msg = $translate.instant('login.password_lost_sent_message', { username: username });
        var btns = [{result: 'ok', label: $translate.instant('ok'), cssClass: 'btn-primary'}];
        $dialog.messageBox(title, msg, btns);
      }, function () {
        var title = $translate.instant('login.password_lost_error_title');
        var msg = $translate.instant('login.password_lost_error_message');
        var btns = [{result: 'ok', label: $translate.instant('ok'), cssClass: 'btn-primary'}];
        $dialog.messageBox(title, msg, btns);
      });
    });
  };
});