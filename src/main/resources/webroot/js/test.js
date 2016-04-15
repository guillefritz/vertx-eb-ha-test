var TIMEOUT = 120000;

var app = angular.module('TestApp', ['knalli.angular-vertxbus']);
app.config(['vertxEventBusProvider', 'vertxEventBusServiceProvider',
	function(vertxEventBusProvider, vertxEventBusServiceProvider) {

		vertxEventBusProvider
			.useDebug(true)
	    	.useReconnect()
	    	.useSockJsReconnectInterval(5000)
	    	//.useUrlServer( ruta = window.location.protocol + '//' + window.location.hostname + ':' + 8000 )
	    	;
		
	    vertxEventBusServiceProvider
		    .useDebug(true)
		    .useSockJsStateInterval(30000);

	}])
.controller(
		'TestCtl', 
		function($scope, vertxEventBusService) {
		
			$scope.sesion = Math.round(Math.random()*30000);
			
			$scope.clickme = function() {
				vertxEventBusService.send('click-'+$scope.sesion, '', {timeout: TIMEOUT})
					.then(function (reply) {
						$scope.clickmevalue = reply;
						$scope.errorvalue = '';
					}).catch(function(err) {
						$scope.clickmevalue = '';
						$scope.errorvalue = err;
					});
			};
			
			$scope.deployNewV = function() {
				vertxEventBusService.send('redeployV-'+$scope.sesion, '', {timeout: TIMEOUT})
					.then(function (reply) {
						$scope.errorvalue = '';
					}).catch(function(err) {
						$scope.errorvalue = err;
					});
			};
			
			$scope.deployNewV2 = function() {
				vertxEventBusService.send('redeployV2-'+$scope.sesion, '', {timeout: TIMEOUT})
					.then(function (reply) {
						$scope.errorvalue = '';
					}).catch(function(err) {
						$scope.errorvalue = err;
					});
			};
			
			vertxEventBusService.send('initClick', $scope.sesion, {timeout: TIMEOUT})
				.then(function (reply) {
					$scope.clickmeID = reply;
					$scope.clickmevalue = '';
					$scope.errorvalue = '';
				}).catch(function(err) {
					$scope.clickmevalue = '';
					$scope.errorvalue = err;
				});
		})
;
	